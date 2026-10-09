// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as pdfjsLib from 'pdfjs-dist';
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import {
  FolderOpen,
  FileText,
  ChevronLeft,
  ChevronRight,
  ZoomIn,
  ZoomOut,
  Sparkles,
  Loader2,
  AlertCircle,
  PenTool,
  Copy,
  X,
} from 'lucide-react';

// Initialize PDF.js worker
if (typeof window !== 'undefined') {
  pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorkerUrl;
}

interface PdfViewerProps {
  pdfData?: Uint8Array | ArrayBuffer | string | null;
  documentTitle?: string;
  onTextExtracted?: (text: string) => void;
  onInsertToEditor?: (text: string) => void;
  onLoadNewPdf?: (data: Uint8Array, name: string) => void;
}

export const PdfViewer: React.FC<PdfViewerProps> = ({
  pdfData,
  documentTitle = 'Lecture_Slides.pdf',
  onTextExtracted,
  onInsertToEditor,
  onLoadNewPdf,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const [pdfDoc, setPdfDoc] = useState<pdfjsLib.PDFDocumentProxy | null>(null);
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [scale, setScale] = useState<number>(1.2);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [extractedOcrText, setExtractedOcrText] = useState<string>('');
  const [showOcrPanel, setShowOcrPanel] = useState<boolean>(false);
  const [copyFeedback, setCopyFeedback] = useState<string | null>(null);

  const currentRenderTaskRef = useRef<pdfjsLib.RenderTask | null>(null);

  // Load PDF Document
  useEffect(() => {
    let isCancelled = false;

    async function loadPdf() {
      if (!pdfData) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        let loadingTask: pdfjsLib.PDFDocumentLoadingTask;

        if (typeof pdfData === 'string') {
          loadingTask = pdfjsLib.getDocument({ url: pdfData });
        } else {
          // Uint8Array or ArrayBuffer
          const bytes = pdfData instanceof Uint8Array ? pdfData : new Uint8Array(pdfData);
          loadingTask = pdfjsLib.getDocument({ data: bytes });
        }

        const doc = await loadingTask.promise;
        if (isCancelled) return;

        setPdfDoc(doc);
        setTotalPages(doc.numPages);
        setCurrentPage(1);
        setIsLoading(false);
      } catch (err: any) {
        if (!isCancelled) {
          console.error('[PdfViewer] Failed to load PDF:', err);
          setError(err?.message || 'Failed to load PDF document');
          setIsLoading(false);
        }
      }
    }

    loadPdf();

    return () => {
      isCancelled = true;
    };
  }, [pdfData]);

  // Render Page to Canvas & Extract Text
  const renderCurrentPage = useCallback(async () => {
    if (!pdfDoc || !canvasRef.current) return;

    try {
      // Cancel ongoing render
      if (currentRenderTaskRef.current) {
        try {
          currentRenderTaskRef.current.cancel();
        } catch {
          // ignore cancel error
        }
      }

      const page = await pdfDoc.getPage(currentPage);
      const canvas = canvasRef.current;
      if (!canvas) return;

      const context = canvas.getContext('2d');
      if (!context) return;

      const viewport = page.getViewport({ scale });
      const pixelRatio = window.devicePixelRatio || 1;

      // Handle HiDPI screens
      canvas.width = Math.floor(viewport.width * pixelRatio);
      canvas.height = Math.floor(viewport.height * pixelRatio);
      canvas.style.width = `${Math.floor(viewport.width)}px`;
      canvas.style.height = `${Math.floor(viewport.height)}px`;

      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);

      const renderContext: any = {
        canvasContext: context,
        viewport,
        canvas,
      };

      const renderTask = page.render(renderContext);
      currentRenderTaskRef.current = renderTask;
      await renderTask.promise;

      // Extract real text from page
      const textContent = await page.getTextContent();
      const pageText = textContent.items
        .map((item: any) => item.str || '')
        .filter((str: string) => str.trim().length > 0)
        .join(' ');

      setExtractedOcrText(pageText);
      if (onTextExtracted) {
        onTextExtracted(pageText);
      }
    } catch (err: any) {
      if (err?.name !== 'RenderingCancelledException') {
        console.error('[PdfViewer] Page render error:', err);
      }
    }
  }, [pdfDoc, currentPage, scale, onTextExtracted]);

  useEffect(() => {
    renderCurrentPage();
  }, [renderCurrentPage]);

  // Page Controls
  const handlePrevPage = () => {
    if (currentPage > 1) setCurrentPage((p) => p - 1);
  };

  const handleNextPage = () => {
    if (currentPage < totalPages) setCurrentPage((p) => p + 1);
  };

  // Zoom Controls
  const handleZoomIn = () => setScale((s) => Math.min(3.0, +(s + 0.2).toFixed(1)));
  const handleZoomOut = () => setScale((s) => Math.max(0.5, +(s - 0.2).toFixed(1)));
  const handleZoomReset = () => setScale(1.2);

  // File picker handler
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      if (reader.result && onLoadNewPdf) {
        const bytes = new Uint8Array(reader.result as ArrayBuffer);
        onLoadNewPdf(bytes, file.name);
      }
    };
    reader.readAsArrayBuffer(file);
    e.target.value = '';
  };

  const copyOcrToClipboard = () => {
    if (!extractedOcrText) return;
    navigator.clipboard.writeText(extractedOcrText);
    setCopyFeedback('Copied to Clipboard!');
    setTimeout(() => setCopyFeedback(null), 2000);
  };

  const insertOcrIntoNote = () => {
    if (!extractedOcrText) return;
    if (onInsertToEditor) {
      onInsertToEditor(extractedOcrText);
      setCopyFeedback('Appended to LaTeX Editor!');
      setTimeout(() => setCopyFeedback(null), 2000);
    }
  };

  return (
    <div className="pdf-viewer-root" ref={containerRef}>
      {/* Top PDF Navigation Toolbar */}
      <div className="pdf-nav-toolbar">
        <div className="pdf-toolbar-group left">
          <button
            className="pdf-btn icon-btn"
            onClick={() => fileInputRef.current?.click()}
            title="Open Local PDF File"
          >
            <FolderOpen size={13} />
            <span>Open</span>
          </button>
          <input
            type="file"
            ref={fileInputRef}
            accept="application/pdf"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
          <span className="pdf-doc-title-badge" title={documentTitle}>
            <FileText size={13} />
            <span>{documentTitle}</span>
          </span>
        </div>

        {/* Page Switcher */}
        <div className="pdf-toolbar-group center">
          <button
            className="pdf-btn"
            onClick={handlePrevPage}
            disabled={currentPage <= 1 || isLoading}
            title="Previous Page"
          >
            <ChevronLeft size={14} />
          </button>
          <span className="pdf-page-indicator">
            Page <b>{currentPage}</b> of <b>{totalPages || 1}</b>
          </span>
          <button
            className="pdf-btn"
            onClick={handleNextPage}
            disabled={currentPage >= totalPages || isLoading}
            title="Next Page"
          >
            <ChevronRight size={14} />
          </button>
        </div>

        {/* Zoom & OCR Controls */}
        <div className="pdf-toolbar-group right">
          <button className="pdf-btn" onClick={handleZoomOut} title="Zoom Out (-)">
            <ZoomOut size={14} />
          </button>
          <span className="pdf-zoom-badge" onClick={handleZoomReset} title="Reset Zoom">
            {Math.round(scale * 100)}%
          </span>
          <button className="pdf-btn" onClick={handleZoomIn} title="Zoom In (+)">
            <ZoomIn size={14} />
          </button>
          <button
            className={`pdf-btn ocr-toggle-btn ${showOcrPanel ? 'active' : ''}`}
            onClick={() => setShowOcrPanel(!showOcrPanel)}
            title="Toggle Live OCR Extracted Text"
          >
            <Sparkles size={13} />
            <span>OCR</span>
          </button>
        </div>
      </div>

      {/* Main PDF Canvas Scroll Viewport */}
      <div className="pdf-canvas-container">
        {isLoading && (
          <div className="pdf-status-banner">
            <Loader2 size={16} className="spinner" /> Rendering PDF Document...
          </div>
        )}

        {error && (
          <div className="pdf-error-banner">
            <AlertCircle size={16} className="error-icon" />
            <span>{error}</span>
            <button className="pdf-btn retry-btn" onClick={renderCurrentPage}>
              Retry
            </button>
          </div>
        )}

        <div className="pdf-canvas-wrapper">
          <canvas ref={canvasRef} className="pdf-render-canvas" />
        </div>
      </div>

      {/* Extracted OCR Bottom Drawer */}
      {showOcrPanel && (
        <div className="pdf-ocr-drawer">
          <div className="ocr-drawer-header">
            <div className="ocr-header-left">
              <span className="ocr-badge">
                <Sparkles size={12} />
                <span>ON-DEVICE OCR EXTRACTION (PAGE {currentPage})</span>
              </span>
              {copyFeedback && <span className="ocr-feedback-pill">{copyFeedback}</span>}
            </div>
            <div className="ocr-header-actions">
              <button
                className="ocr-action-btn"
                onClick={insertOcrIntoNote}
                disabled={!extractedOcrText}
                title="Send Extracted Text into LaTeX Note Editor"
              >
                <PenTool size={13} />
                <span>Append to Note</span>
              </button>
              <button
                className="ocr-action-btn"
                onClick={copyOcrToClipboard}
                disabled={!extractedOcrText}
                title="Copy to Clipboard"
              >
                <Copy size={13} />
                <span>Copy</span>
              </button>
              <button
                className="ocr-close-btn"
                onClick={() => setShowOcrPanel(false)}
                title="Close OCR Panel"
              >
                <X size={14} />
              </button>
            </div>
          </div>
          <div className="ocr-drawer-body">
            {extractedOcrText ? (
              <p className="ocr-text-content">{extractedOcrText}</p>
            ) : (
              <span className="ocr-empty-text">No selectable text found on this page.</span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
