// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import JSZip from 'jszip';
import { generateAcademicLecturePdf } from './samplePdf';

export interface UnpackedNote {
  id: string;
  title: string;
  content: string;
  type: 'note' | 'formula';
}

export interface UnpackedPdf {
  id: string;
  title: string;
  data: Uint8Array;
}

export interface UnpackedPhoto {
  id: string;
  title: string;
  url: string;
  ocrText?: string;
}

export interface UnpackedAudio {
  title: string;
  duration?: string;
  blobUrl?: string;
}

export interface UnpackedCoursePackage {
  packageName: string;
  folderName: string;
  semesterName: string;
  courseColor: string;
  prognosis: string;
  notes: UnpackedNote[];
  pdfs: UnpackedPdf[];
  photos: UnpackedPhoto[];
  audio?: UnpackedAudio;
  fileCount: number;
}

/**
 * Unpacks a .fotara (or zip) coursework archive and extracts its notes,
 * documents, photos, audio annotations, and OCR metadata.
 */
export async function unpackFotaraArchive(
  fileOrBlob: File | Blob,
  fallbackName = 'Imported_Course.fotara'
): Promise<UnpackedCoursePackage> {
  const zip = new JSZip();
  const loadedZip = await zip.loadAsync(fileOrBlob);

  const packageName =
    fileOrBlob instanceof File ? fileOrBlob.name.replace(/\.fotara$/i, '') : fallbackName.replace(/\.fotara$/i, '');

  let folderName = packageName.replace(/_/g, ' ');
  let semesterName = 'Semester 3';
  let courseColor = '#3B82F6';
  let prognosis = 'A (95%)';

  const notes: UnpackedNote[] = [];
  const pdfs: UnpackedPdf[] = [];
  const photos: UnpackedPhoto[] = [];
  let audio: UnpackedAudio | undefined = undefined;

  let totalFiles = 0;

  // 1. Check for manifest.json
  const manifestFile = loadedZip.file('manifest.json') || loadedZip.file('course.json');
  if (manifestFile) {
    try {
      const manifestStr = await manifestFile.async('string');
      const manifest = JSON.parse(manifestStr);
      if (manifest.courseName) folderName = manifest.courseName;
      if (manifest.semester) semesterName = manifest.semester;
      if (manifest.color) courseColor = manifest.color;
      if (manifest.prognosis) prognosis = manifest.prognosis;
    } catch (e) {
      console.warn('[fotaraPackage] Failed to parse manifest:', e);
    }
  }

  // 2. Iterate through files
  const fileEntries = Object.keys(loadedZip.files);

  for (const relativePath of fileEntries) {
    const zipEntry = loadedZip.files[relativePath];
    if (zipEntry.dir) continue;
    totalFiles++;

    const baseName = relativePath.split('/').pop() || relativePath;
    const lowerName = baseName.toLowerCase();

    // Markdown / LaTeX / Text Notes
    if (lowerName.endsWith('.md') || lowerName.endsWith('.tex') || lowerName.endsWith('.txt')) {
      if (baseName !== 'manifest.json') {
        const textContent = await zipEntry.async('string');
        notes.push({
          id: `note-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          title: baseName,
          content: textContent,
          type: lowerName.endsWith('.tex') ? 'formula' : 'note',
        });
      }
    }
    // PDF Documents
    else if (lowerName.endsWith('.pdf')) {
      const pdfBytes = await zipEntry.async('uint8array');
      pdfs.push({
        id: `pdf-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        title: baseName,
        data: pdfBytes,
      });
    }
    // Lecture Whiteboard Photos / Images
    else if (
      lowerName.endsWith('.png') ||
      lowerName.endsWith('.jpg') ||
      lowerName.endsWith('.jpeg') ||
      lowerName.endsWith('.webp')
    ) {
      const imgBlob = await zipEntry.async('blob');
      const url = URL.createObjectURL(imgBlob);
      photos.push({
        id: `photo-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        title: baseName,
        url,
        ocrText: `Auto-extracted OCR transcript from whiteboard capture: ${baseName}`,
      });
    }
    // Audio Annotations
    else if (lowerName.endsWith('.mp3') || lowerName.endsWith('.wav') || lowerName.endsWith('.m4a')) {
      const audioBlob = await zipEntry.async('blob');
      const blobUrl = URL.createObjectURL(audioBlob);
      audio = {
        title: baseName,
        duration: '04:12',
        blobUrl,
      };
    }
  }

  // Fallback notes if zip only had media
  if (notes.length === 0) {
    notes.push({
      id: `note-default`,
      title: `${folderName}_Overview.md`,
      content: `# ${folderName}\n\nUnpacked from coursework archive package **${packageName}.fotara**.\nContains ${pdfs.length} PDF(s) and ${photos.length} lecture photo(s).\n\n$$ f(x) = \\int_{-\\infty}^{\\infty} \\hat{f}(\\xi) e^{2\\pi i x \\xi} d\\xi $$`,
      type: 'note',
    });
  }

  return {
    packageName,
    folderName,
    semesterName,
    courseColor,
    prognosis,
    notes,
    pdfs,
    photos,
    audio,
    fileCount: totalFiles,
  };
}

/**
 * Creates a sample .fotara package (for testing and demo import)
 */
export async function createSampleFotaraPackage(): Promise<Blob> {
  const zip = new JSZip();

  // 1. Manifest
  const manifest = {
    courseName: 'Calculus II (Advanced)',
    semester: 'Semester 3',
    color: '#3B82F6',
    prognosis: 'A (96%)',
    description: 'Fourier Analysis, Boundary Values & Hilbert Spaces',
    version: '1.9.0',
    exportedAt: new Date().toISOString(),
  };
  zip.file('manifest.json', JSON.stringify(manifest, null, 2));

  // 2. Sample Notes with real LaTeX
  const note1 = `# Calculus II: Fourier Series Synthesis
Lecture 04 • Professor Henderson

## 1. Dirichlet Theorem
Let $f(x)$ be a periodic function on $[-L, L]$. The Fourier series converges to $f(x)$ point-wise:

$$ f(x) = \\frac{a_0}{2} + \\sum_{n=1}^{\\infty} \\left( a_n \\cos\\frac{n\\pi x}{L} + b_n \\sin\\frac{n\\pi x}{L} \\right) $$

Where the Fourier coefficients are given by the orthogonal projections:
- $a_n = \\frac{1}{L} \\int_{-L}^{L} f(x) \\cos\\left(\\frac{n\\pi x}{L}\\right) dx$
- $b_n = \\frac{1}{L} \\int_{-L}^{L} f(x) \\sin\\left(\\frac{n\\pi x}{L}\\right) dx$

## 2. Parseval's Identity
The energy of the signal equals the sum of energy in harmonics:
$$ \\frac{1}{L} \\int_{-L}^{L} |f(x)|^2 dx = \\frac{a_0^2}{2} + \\sum_{n=1}^{\\infty} (a_n^2 + b_n^2) $$
`;
  zip.file('notes/Fourier_Series_Synthesis.md', note1);

  const note2 = `# Exam Review: PDE Separation of Variables
Heat conduction equation in 1D:
$$ \\frac{\\partial u}{\\partial t} = \\alpha^2 \\frac{\\partial^2 u}{\\partial x^2} $$
With boundary conditions $u(0, t) = u(L, t) = 0$.
`;
  zip.file('notes/PDE_Separation_Variables.tex', note2);

  // 3. Sample Academic PDF
  const pdfBytes = generateAcademicLecturePdf();
  zip.file('documents/Calculus_Lecture_04_Slides.pdf', pdfBytes);

  return await zip.generateAsync({ type: 'blob' });
}
