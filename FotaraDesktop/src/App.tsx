// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import React, { useState, useEffect, useRef } from 'react';
import {
  Home,
  FileText,
  Settings,
  Plus,
  Search,
  MoreVertical,
  Folder,
  LayoutGrid,
  List,
  Archive,
  HardDrive,
  ArrowRight,
  Minus,
  Square,
  X,
  Sparkles,
  Maximize2,
  ChevronLeft,
  Upload,
  BookOpen,
  Package,
  Command,
  Play,
  Pause,
  FolderOpen,
} from 'lucide-react';
import './App.css';
import { PdfViewer } from './components/PdfViewer';
import { MathRenderer } from './components/MathRenderer';
import { NotesListView } from './components/NotesListView';
import type { UnifiedDesktopNote } from './components/NotesListView';
import { FolderDetailView } from './components/FolderDetailView';
import { generateAcademicLecturePdf } from './utils/samplePdf';
import { unpackFotaraArchive, createSampleFotaraPackage } from './utils/fotaraPackage';

interface CourseFolder {
  id: string;
  name: string;
  notesCount: number;
  color: string;
  tileFill: string;
  iconTint: string;
  glowColor: string;
  isLinked?: boolean;
  linkedFolderName?: string;
}

const FileTypeBadge: React.FC<{ type: 'canvas' | 'txt' | 'docx' | 'pdf' | 'photo' }> = ({ type }) => {
  if (type === 'canvas') {
    return (
      <div className="note-type-badge canvas-badge" title="Canvas Note">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="m15 5 4 4" />
          <path d="M13 7 4 16v4h4l9-9z" />
          <path d="m18 2 4 4-2 2-4-4z" />
        </svg>
      </div>
    );
  }

  return (
    <div className={`note-type-badge ${type}-badge`} title={`${type.toUpperCase()} Document`}>
      <svg width="22" height="24" viewBox="0 0 24 28" fill="none" className="doc-badge-svg">
        <path
          d="M3 3C3 1.89543 3.89543 1 5 1H15L21 7V25C21 26.1046 20.1046 27 19 27H5C3.89543 27 3 26.1046 3 25V3Z"
          fill="#141E33"
          stroke="#3B82F6"
          strokeWidth="1.5"
        />
        <path d="M15 1V7H21" stroke="#3B82F6" strokeWidth="1.5" />
        <text
          x="12"
          y="20"
          textAnchor="middle"
          fill="#93C5FD"
          fontSize="7"
          fontWeight="bold"
          fontFamily="system-ui, sans-serif"
          letterSpacing="0.2px"
        >
          {type.toUpperCase()}
        </text>
      </svg>
    </div>
  );
};

export const App: React.FC = () => {
  // Navigation & View State
  const [activeNav, setActiveNav] = useState<'home' | 'notes' | 'folder' | 'studio' | 'settings'>('home');
  const [activeWorkspace, setActiveWorkspace] = useState<string>('Home');
  const [folderViewMode, setFolderViewMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedFolderForDetail, setSelectedFolderForDetail] = useState<CourseFolder | null>(null);

  // Hotkey & Layout States
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(true);
  const [isCommandPaletteOpen, setIsCommandPaletteOpen] = useState<boolean>(false);
  const [isZenMode, setIsZenMode] = useState<boolean>(false);

  // Studio Mode State
  const [studioSplitMode, setStudioSplitMode] = useState<'split' | 'pdf' | 'editor'>('split');
  const [currentDocTitle, setCurrentDocTitle] = useState<string>('Jadwal_PSTS_Ganjil_2026-2027.pdf');
  const [pdfData, setPdfData] = useState<Uint8Array | null>(null);
  const [latexFormula, setLatexFormula] = useState<string>(
    '# Calculus II: Fourier Series Synthesis\n\n' +
    'The Dirichlet conditions guarantee point-wise convergence:\n\n' +
    '$$ f(x) = \\frac{a_0}{2} + \\sum_{n=1}^{\\infty} \\left[ a_n \\cos\\left(\\frac{n\\pi x}{L}\\right) + b_n \\sin\\left(\\frac{n\\pi x}{L}\\right) \\right] $$\n\n' +
    'Where orthogonal projections evaluate Fourier coefficients:\n' +
    '- $a_n = \\frac{1}{L} \\int_{-L}^{L} f(x) \\cos\\left(\\frac{n\\pi x}{L}\\right) dx$\n' +
    '- $b_n = \\frac{1}{L} \\int_{-L}^{L} f(x) \\sin\\left(\\frac{n\\pi x}{L}\\right) dx$'
  );

  // Audio Scrubber State
  const [isPlayingAudio, setIsPlayingAudio] = useState<boolean>(false);
  const [audioProgress, setAudioProgress] = useState<number>(38);

  // Drag and Drop & Toast State
  const [isDraggingOver, setIsDraggingOver] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Modals
  const [isNewFolderModalOpen, setIsNewFolderModalOpen] = useState<boolean>(false);
  const [newFolderName, setNewFolderName] = useState<string>('');
  const [newFolderColor, setNewFolderColor] = useState<string>('#3B82F6');

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Initialize sample PDF on boot
  useEffect(() => {
    try {
      const initialPdf = generateAcademicLecturePdf();
      setPdfData(initialPdf);
    } catch (e) {
      console.error('[App] Failed to generate initial PDF buffer:', e);
    }
  }, []);

  // Course Folders (Strictly matching Android FolderAccentPalette and Image)
  const [folders, setFolders] = useState<CourseFolder[]>([
    {
      id: 'f-1',
      name: 'Jadwal Psts',
      notesCount: 1,
      color: '#EF4444',
      tileFill: '#6B2A30',
      iconTint: '#F87171',
      glowColor: '#EF4444',
    },
    {
      id: 'f-2',
      name: 'Sejarah',
      notesCount: 3,
      color: '#3B82F6',
      tileFill: '#343C52',
      iconTint: '#94A3B8',
      glowColor: '#38BDF8',
    },
    {
      id: 'f-3',
      name: 'Seni Budaya',
      notesCount: 4,
      color: '#A855F7',
      tileFill: '#3F3270',
      iconTint: '#C084FC',
      glowColor: '#8B5CF6',
    },
    {
      id: 'f-4',
      name: 'Matematika Lanjut',
      notesCount: 1,
      color: '#F59E0B',
      tileFill: '#5C4030',
      iconTint: '#FBBF24',
      glowColor: '#F59E0B',
      isLinked: true,
      linkedFolderName: 'Matematika Wajib',
    },
    {
      id: 'f-5',
      name: 'Matematika Wajib',
      notesCount: 3,
      color: '#8B5CF6',
      tileFill: '#3F3270',
      iconTint: '#C084FC',
      glowColor: '#8B5CF6',
      isLinked: true,
      linkedFolderName: 'Matematika Lanjut',
    },
    {
      id: 'f-6',
      name: 'Fisika',
      notesCount: 4,
      color: '#10B981',
      tileFill: '#1E4A38',
      iconTint: '#4ADE80',
      glowColor: '#34D399',
    },
  ]);

  // Unified Notes List (Connecting Home, NotesScreen, and FolderDetail)
  const [notes, setNotes] = useState<UnifiedDesktopNote[]>([
    {
      id: 'n-1',
      title: 'Canvas Note',
      folder: 'Sejarah',
      timestamp: '10:21',
      dateGroup: 'today',
      type: 'canvas',
      content: '# Sejarah Indonesia: Jalur Rempah & Masa Kolonial\n\nPeta rute navigasi rempah kepulauan Maluku abad ke-17.',
    },
    {
      id: 'n-2',
      title: 'Canvas Note',
      folder: 'Seni Budaya',
      timestamp: 'Yesterday • 19:05',
      dateGroup: 'yesterday',
      type: 'canvas',
      content: '# Seni Rupa Tradisional & Kontemporer\n\nStudi ornamen ragam hias nusantara dan proporsi geometris.',
    },
    {
      id: 'n-3',
      title: 'testb',
      folder: 'Seni Budaya',
      timestamp: 'Yesterday • 19:05',
      dateGroup: 'yesterday',
      type: 'txt',
      content: 'Persiapan Penilaian Tengah Semester: Tangga nada diatonis mayor dan minor.',
    },
    {
      id: 'n-4',
      title: 'KISI-KISI PSTS GANJIL XI T...',
      folder: 'Seni Budaya',
      timestamp: 'Yesterday • 19:04',
      dateGroup: 'yesterday',
      type: 'docx',
      content: 'Kisi-kisi soal evaluasi teori musik barat, harmoni vokal, dan apresiasi karya seni rupa.',
    },
    {
      id: 'n-5',
      title: 'Jadwal_PSTS_Ganjil_2026-2...',
      folder: 'Jadwal Psts',
      timestamp: 'Yesterday • 18:09',
      dateGroup: 'yesterday',
      type: 'pdf',
    },
    {
      id: 'n-6',
      title: 'Fourier_Series_Derivation.md',
      folder: 'Matematika Lanjut',
      timestamp: 'Yesterday • 16:30',
      dateGroup: 'yesterday',
      type: 'txt',
      content: '# Fourier Series & Heat Conduction\n\n$$ f(x) = \\frac{a_0}{2} + \\sum_{n=1}^{\\infty} a_n \\cos\\frac{n\\pi x}{L} $$',
    },
    {
      id: 'n-7',
      title: 'Fisika_Mekanika_Fluida.pdf',
      folder: 'Fisika',
      timestamp: 'Oct 7 • 14:10',
      dateGroup: 'earlier',
      type: 'pdf',
    },
  ]);

  // Toast Helper
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3500);
  };

  // Keyboard Shortcuts: Ctrl+B (Sidebar), Ctrl+K (Palette), F11/Zen, Esc
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
        e.preventDefault();
        setIsSidebarOpen((prev) => !prev);
      }
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setIsCommandPaletteOpen((prev) => !prev);
      }
      if (e.key === 'F11' || ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'z')) {
        e.preventDefault();
        setIsZenMode((prev) => !prev);
      }
      if (e.key === 'Escape') {
        if (isCommandPaletteOpen) {
          setIsCommandPaletteOpen(false);
        } else if (isZenMode) {
          setIsZenMode(false);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isCommandPaletteOpen, isZenMode]);

  // Global Drag & Drop listener for .fotara and .pdf files
  useEffect(() => {
    const handleDragOver = (e: DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDraggingOver(true);
    };

    const handleDragLeave = (e: DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      if (e.relatedTarget === null) {
        setIsDraggingOver(false);
      }
    };

    const handleDrop = async (e: DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDraggingOver(false);

      if (!e.dataTransfer || !e.dataTransfer.files || e.dataTransfer.files.length === 0) {
        return;
      }

      const file = e.dataTransfer.files[0];
      await processUploadedFile(file);
    };

    window.addEventListener('dragover', handleDragOver);
    window.addEventListener('dragleave', handleDragLeave);
    window.addEventListener('drop', handleDrop);

    return () => {
      window.removeEventListener('dragover', handleDragOver);
      window.removeEventListener('dragleave', handleDragLeave);
      window.removeEventListener('drop', handleDrop);
    };
  }, [folders]);

  // Process uploaded .fotara package or .pdf file
  const processUploadedFile = async (file: File) => {
    const lowerName = file.name.toLowerCase();

    if (lowerName.endsWith('.fotara') || lowerName.endsWith('.zip')) {
      showToast(`Unpacking coursework package: ${file.name}...`);
      try {
        const unpacked = await unpackFotaraArchive(file);

        const newFolder: CourseFolder = {
          id: `f-${Date.now()}`,
          name: unpacked.folderName,
          notesCount: unpacked.notes.length + unpacked.pdfs.length,
          color: unpacked.courseColor,
          tileFill: '#1E3A6B',
          iconTint: '#60A5FA',
          glowColor: unpacked.courseColor,
        };

        setFolders((prev) => [newFolder, ...prev]);

        const extractedItems: UnifiedDesktopNote[] = [];
        unpacked.notes.forEach((un) => {
          extractedItems.push({
            id: un.id,
            title: un.title,
            folder: unpacked.folderName,
            timestamp: 'Just now',
            dateGroup: 'today',
            type: un.type === 'formula' ? 'txt' : 'canvas',
            content: un.content,
          });
        });

        unpacked.pdfs.forEach((up) => {
          extractedItems.push({
            id: up.id,
            title: up.title,
            folder: unpacked.folderName,
            timestamp: 'Just now',
            dateGroup: 'today',
            type: 'pdf',
          });
          setPdfData(up.data);
          setCurrentDocTitle(up.title);
        });

        setNotes((prev) => [...extractedItems, ...prev]);
        showToast(`Unpacked ${file.name} (${unpacked.fileCount} items extracted)!`);
      } catch (err: any) {
        console.error('Failed to unpack .fotara archive:', err);
        showToast(`Error unpacking archive: ${err?.message || 'Invalid format'}`);
      }
    } else if (lowerName.endsWith('.pdf')) {
      showToast(`Loading PDF document: ${file.name}...`);
      const reader = new FileReader();
      reader.onload = () => {
        if (reader.result) {
          const bytes = new Uint8Array(reader.result as ArrayBuffer);
          setPdfData(bytes);
          setCurrentDocTitle(file.name);

          const pdfNote: UnifiedDesktopNote = {
            id: `n-${Date.now()}`,
            title: file.name,
            folder: folders[0]?.name || 'Downloads',
            timestamp: 'Just now',
            dateGroup: 'today',
            type: 'pdf',
          };
          setNotes((prev) => [pdfNote, ...prev]);
          setActiveNav('studio');
          showToast(`Opened ${file.name} in PDF Studio!`);
        }
      };
      reader.readAsArrayBuffer(file);
    } else {
      showToast(`File received: ${file.name}`);
    }
  };

  // Open a note into the Widescreen Studio
  const handleOpenNote = (note: UnifiedDesktopNote) => {
    setCurrentDocTitle(note.title);
    if (note.content) {
      setLatexFormula(note.content);
    }
    setActiveNav('studio');
  };

  // Open folder details
  const handleOpenFolder = (folderName: string) => {
    const matched = folders.find((f) => f.name.toLowerCase() === folderName.toLowerCase());
    if (matched) {
      setSelectedFolderForDetail(matched);
      setActiveNav('folder');
    } else {
      showToast(`Folder: ${folderName}`);
    }
  };

  // Create new folder
  const handleCreateFolder = () => {
    if (!newFolderName.trim()) return;
    const newFolder: CourseFolder = {
      id: `f-${Date.now()}`,
      name: newFolderName.trim(),
      notesCount: 0,
      color: newFolderColor,
      tileFill: '#1E3A6B',
      iconTint: '#60A5FA',
      glowColor: newFolderColor,
    };
    setFolders((prev) => [...prev, newFolder]);
    setNewFolderName('');
    setIsNewFolderModalOpen(false);
    showToast(`Created folder: ${newFolder.name}`);
  };

  // Demo .fotara load
  const handleLoadDemoPackage = async () => {
    showToast('Generating and unpacking demo Calculus.fotara package...');
    try {
      const blob = await createSampleFotaraPackage();
      const file = new File([blob], 'Calculus_II_Semester3.fotara', { type: 'application/zip' });
      await processUploadedFile(file);
    } catch (e: any) {
      console.error(e);
      showToast('Failed to create demo package.');
    }
  };

  // Filtered folders and notes for Home view
  const filteredFolders = folders.filter((f) =>
    f.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const homeRecentNotes = notes.slice(0, 5);

  return (
    <div className={`fotara-root-container ${isZenMode ? 'zen-active' : ''}`}>
      {/* ─────────────────────────────────────────────────────────────
          1. LEFT SIDEBAR (NAVIGATION, WORKSPACES, STORAGE WIDGET)
      ───────────────────────────────────────────────────────────── */}
      {isSidebarOpen && !isZenMode && (
        <aside className="fotara-sidebar">
          {/* Top Brand Logo */}
          <div className="sidebar-brand-section" onClick={() => setActiveNav('home')}>
            <div className="fotara-logo-tile">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M4 4L20 4L12 20L4 4Z" fill="#FFFFFF" />
              </svg>
            </div>
            <span className="fotara-brand-title">Fotara</span>
          </div>

          {/* Primary Navigation Menu */}
          <nav className="sidebar-main-nav">
            <button
              className={`nav-menu-item ${activeNav === 'home' ? 'active' : ''}`}
              onClick={() => setActiveNav('home')}
            >
              <Home size={18} className="nav-item-icon" />
              <span className="nav-item-label">Home</span>
            </button>

            <button
              className={`nav-menu-item ${activeNav === 'notes' ? 'active' : ''}`}
              onClick={() => setActiveNav('notes')}
            >
              <FileText size={18} className="nav-item-icon" />
              <span className="nav-item-label">Notes</span>
            </button>

            <button
              className={`nav-menu-item ${activeNav === 'settings' ? 'active' : ''}`}
              onClick={() => setActiveNav('settings')}
            >
              <Settings size={18} className="nav-item-icon" />
              <span className="nav-item-label">Settings</span>
            </button>
          </nav>

          {/* Workspaces Group */}
          <div className="sidebar-workspaces-section">
            <div className="workspaces-header-row">
              <span className="workspaces-section-title">Workspaces</span>
              <button
                className="btn-add-workspace"
                title="Add Workspace"
                onClick={() => showToast('New workspace')}
              >
                <Plus size={15} />
              </button>
            </div>

            <div className="workspaces-items-list">
              <button
                className={`workspace-row-btn ${activeWorkspace === 'Home' ? 'active' : ''}`}
                onClick={() => {
                  setActiveWorkspace('Home');
                  setActiveNav('home');
                }}
              >
                <LayoutGrid size={16} className="workspace-icon" />
                <span className="workspace-label">Home</span>
              </button>

              <button
                className={`workspace-row-btn ${activeWorkspace === 'Archive' ? 'active' : ''}`}
                onClick={() => {
                  setActiveWorkspace('Archive');
                  showToast('Opened Archive workspace');
                }}
              >
                <Archive size={16} className="workspace-icon" />
                <span className="workspace-label">Archive</span>
              </button>

              <button
                className={`workspace-row-btn ${activeWorkspace === 'Test' ? 'active' : ''}`}
                onClick={() => {
                  setActiveWorkspace('Test');
                  showToast('Opened Test workspace');
                }}
              >
                <Folder size={16} className="workspace-icon" />
                <span className="workspace-label">Test</span>
              </button>

              <button
                className={`workspace-row-btn ${activeWorkspace === 'test' ? 'active' : ''}`}
                onClick={() => {
                  setActiveWorkspace('test');
                  showToast('Opened test workspace');
                }}
              >
                <Folder size={16} className="workspace-icon" />
                <span className="workspace-label">test</span>
              </button>
            </div>
          </div>

          {/* Bottom Storage Status Card */}
          <div
            className="sidebar-storage-card"
            onClick={() => setActiveNav('settings')}
            title="Manage Storage Residue & Disk Usage"
          >
            <div className="storage-card-header">
              <HardDrive size={18} className="storage-drive-icon" />
              <div className="storage-text-block">
                <span className="storage-title">Storage</span>
                <span className="storage-details">2.4 GB of 10 GB used</span>
              </div>
            </div>
            <div className="storage-progress-track">
              <div className="storage-progress-bar" style={{ width: '24%' }} />
            </div>
          </div>
        </aside>
      )}

      {/* ─────────────────────────────────────────────────────────────
          2. MAIN WORKSPACE / DASHBOARD AREA
      ───────────────────────────────────────────────────────────── */}
      <main className="fotara-main-viewport">
        {/* Window Top Controls Bar */}
        <div className="window-top-bar">
          <div className="window-left-actions">
            {!isSidebarOpen && !isZenMode && (
              <button
                className="btn-window-icon"
                onClick={() => setIsSidebarOpen(true)}
                title="Show Sidebar (Ctrl+B)"
              >
                <LayoutGrid size={15} />
              </button>
            )}
          </div>
          <div className="window-control-buttons">
            <button className="window-ctrl-btn" title="Minimize">
              <Minus size={13} />
            </button>
            <button
              className="window-ctrl-btn"
              title="Zen Mode / Maximize"
              onClick={() => setIsZenMode(!isZenMode)}
            >
              <Square size={12} />
            </button>
            <button className="window-ctrl-btn close" title="Close">
              <X size={14} />
            </button>
          </div>
        </div>

        {/* VIEW 1: HOME VIEW (EXACT CANONICAL REPLICA OF THE IMAGE) */}
        {activeNav === 'home' && (
          <div className="home-dashboard-scroll">
            {/* Header Section */}
            <header className="home-dashboard-header">
              <div className="header-titles-block">
                <h1 className="home-main-title">Home</h1>
                <p className="home-subtitle">Your notes, organized</p>
              </div>

              <div className="header-search-actions">
                <div className="home-search-pill">
                  <Search size={16} className="search-glyph" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search notes, subjects, text..."
                    className="search-input-field"
                  />
                  {searchQuery && (
                    <button
                      className="search-clear-btn"
                      onClick={() => setSearchQuery('')}
                    >
                      <X size={13} />
                    </button>
                  )}
                </div>

                <button
                  className="btn-create-plus"
                  onClick={() => setIsNewFolderModalOpen(true)}
                  title="Create New Folder or Note"
                >
                  <Plus size={20} color="#FFFFFF" />
                </button>

                <button
                  className="btn-more-options"
                  onClick={() => setIsCommandPaletteOpen(true)}
                  title="Command Palette & Quick Actions (Ctrl+K)"
                >
                  <MoreVertical size={18} />
                </button>
              </div>
            </header>

            {/* FOLDERS SECTION */}
            <section className="folders-grid-section">
              <div className="section-title-bar">
                <div className="section-left-heading">
                  <Folder size={17} className="section-heading-icon" />
                  <h2 className="section-heading-text">Folders</h2>
                </div>

                <div className="section-right-tools">
                  <span className="items-counter">{filteredFolders.length} folders</span>
                  <div className="view-mode-toggle-group">
                    <button
                      className={`btn-view-toggle ${folderViewMode === 'grid' ? 'active' : ''}`}
                      onClick={() => setFolderViewMode('grid')}
                      title="Grid View"
                    >
                      <LayoutGrid size={15} />
                    </button>
                    <button
                      className={`btn-view-toggle ${folderViewMode === 'list' ? 'active' : ''}`}
                      onClick={() => setFolderViewMode('list')}
                      title="List View"
                    >
                      <List size={15} />
                    </button>
                  </div>
                </div>
              </div>

              {/* Folders 1:1 Aspect Ratio Grid (Strict Rule R-004) */}
              <div className={`folders-container-layout ${folderViewMode}`}>
                {filteredFolders.map((folder) => (
                  <div
                    key={folder.id}
                    className={`folder-study-card ${folder.isLinked ? 'folder-linkit-linked' : ''}`}
                    onClick={() => handleOpenFolder(folder.name)}
                  >
                    <div className="folder-card-top">
                      <div
                        className="folder-icon-accent-tile"
                        style={{
                          backgroundColor: folder.tileFill,
                          color: folder.iconTint,
                        }}
                      >
                        <Folder size={22} />
                      </div>
                      <button
                        className="folder-card-menu-btn"
                        onClick={(e) => {
                          e.stopPropagation();
                          showToast(`Options: ${folder.name}`);
                        }}
                      >
                        <MoreVertical size={18} />
                      </button>
                    </div>

                    <div className="folder-card-info">
                      <h3 className="folder-card-name">{folder.name}</h3>
                      <p className="folder-card-count">
                        {folder.notesCount} {folder.notesCount === 1 ? 'note' : 'notes'}
                      </p>
                    </div>

                    {/* LinkIt Corner Stroke Glow Indicator */}
                    {folder.isLinked && (
                      <div
                        className="linkit-corner-stroke"
                        style={{ borderColor: folder.glowColor }}
                      />
                    )}
                  </div>
                ))}

                {/* + New Folder Card */}
                <div
                  className="folder-study-card new-folder-dashed-card"
                  onClick={() => setIsNewFolderModalOpen(true)}
                >
                  <div className="new-folder-content">
                    <Plus size={18} className="new-folder-plus-icon" />
                    <span>New Folder</span>
                  </div>
                </div>
              </div>
            </section>

            {/* RECENT NOTES SECTION */}
            <section className="recent-notes-section">
              <div className="section-title-bar">
                <div className="section-left-heading">
                  <FileText size={17} className="section-heading-icon" />
                  <h2 className="section-heading-text">Recent Notes</h2>
                </div>

                <div className="section-right-tools">
                  <span className="items-counter">{homeRecentNotes.length} items</span>
                  <button
                    className="btn-view-all-link"
                    onClick={() => setActiveNav('notes')}
                  >
                    <span>View all</span>
                    <ArrowRight size={14} />
                  </button>
                </div>
              </div>

              {/* Notes List Table */}
              <div className="recent-notes-list-table">
                {homeRecentNotes.map((note) => (
                  <div
                    key={note.id}
                    className="recent-note-row"
                    onClick={() => handleOpenNote(note)}
                  >
                    <div className="note-type-cell">
                      <FileTypeBadge type={note.type} />
                      <span className="note-title-text">{note.title}</span>
                    </div>

                    <div
                      className="note-folder-cell"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleOpenFolder(note.folder);
                      }}
                    >
                      <Folder size={14} className="cell-folder-icon" />
                      <span className="cell-folder-name">{note.folder}</span>
                    </div>

                    <div className="note-time-cell">
                      <span>{note.timestamp}</span>
                    </div>

                    <div className="note-action-cell">
                      <button
                        className="btn-note-row-menu"
                        onClick={(e) => {
                          e.stopPropagation();
                          showToast(`Options: ${note.title}`);
                        }}
                      >
                        <MoreVertical size={16} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}

        {/* VIEW 2: FOLDER DETAIL VIEW */}
        {activeNav === 'folder' && selectedFolderForDetail && (
          <FolderDetailView
            folderName={selectedFolderForDetail.name}
            folderColor={selectedFolderForDetail.color}
            notes={notes}
            isLinked={selectedFolderForDetail.isLinked}
            linkedFolderName={selectedFolderForDetail.linkedFolderName}
            onBack={() => setActiveNav('home')}
            onOpenNote={handleOpenNote}
            onOpenInStudio={() => setActiveNav('studio')}
            onAddNote={() => {
              const newNote: UnifiedDesktopNote = {
                id: `n-${Date.now()}`,
                title: `${selectedFolderForDetail.name}_Note.md`,
                folder: selectedFolderForDetail.name,
                timestamp: 'Just now',
                dateGroup: 'today',
                type: 'txt',
                content: `# ${selectedFolderForDetail.name}\n\nNew coursework note.`,
              };
              setNotes((prev) => [newNote, ...prev]);
              handleOpenNote(newNote);
            }}
          />
        )}

        {/* VIEW 3: UNIFIED NOTES SCREEN (MATCHING ANDROID NotesScreen) */}
        {activeNav === 'notes' && (
          <NotesListView
            notes={notes}
            onOpenNote={handleOpenNote}
            onOpenFolder={handleOpenFolder}
            onCreateNote={() => {
              const newNote: UnifiedDesktopNote = {
                id: `n-${Date.now()}`,
                title: `Lecture_Note_${Date.now().toString().slice(-4)}.md`,
                folder: folders[0]?.name || 'Notes',
                timestamp: 'Just now',
                dateGroup: 'today',
                type: 'txt',
                content: '# New Lecture Note\n\nStart typing LaTeX formulas or markdown...',
              };
              setNotes((prev) => [newNote, ...prev]);
              handleOpenNote(newNote);
            }}
          />
        )}

        {/* VIEW 4: WIDESCREEN STUDY STUDIO (PDF.JS + KATEX SPLIT VIEW) */}
        {activeNav === 'studio' && (
          <div className="studio-workspace-view">
            {/* Studio Navigation Bar */}
            <div className="studio-top-action-bar">
              <div className="studio-left-breadcrumbs">
                <button
                  className="btn-back-to-home"
                  onClick={() => setActiveNav('home')}
                >
                  <ChevronLeft size={16} />
                  <span>Home</span>
                </button>
                <span className="studio-breadcrumb-divider">/</span>
                <span className="studio-current-filename">{currentDocTitle}</span>
              </div>

              <div className="studio-right-view-controls">
                <div className="studio-split-pills">
                  <button
                    className={`split-pill-btn ${studioSplitMode === 'split' ? 'active' : ''}`}
                    onClick={() => setStudioSplitMode('split')}
                  >
                    Split Studio
                  </button>
                  <button
                    className={`split-pill-btn ${studioSplitMode === 'pdf' ? 'active' : ''}`}
                    onClick={() => setStudioSplitMode('pdf')}
                  >
                    PDF Only
                  </button>
                  <button
                    className={`split-pill-btn ${studioSplitMode === 'editor' ? 'active' : ''}`}
                    onClick={() => setStudioSplitMode('editor')}
                  >
                    Editor Only
                  </button>
                </div>

                <button
                  className={`btn-zen-mode-toggle ${isZenMode ? 'active' : ''}`}
                  onClick={() => setIsZenMode(!isZenMode)}
                  title="Distraction-Free Zen Mode (F11 / Ctrl+Shift+Z)"
                >
                  <Maximize2 size={14} />
                  <span>{isZenMode ? 'Exit Zen' : 'Zen View'}</span>
                </button>
              </div>
            </div>

            {/* Split Screen Workspace */}
            <div className={`studio-split-viewport mode-${studioSplitMode}`}>
              {/* Left Pane: PDF Document Viewer */}
              {studioSplitMode !== 'editor' && (
                <div className="studio-pane pdf-pane-container">
                  <PdfViewer
                    pdfData={pdfData}
                    documentTitle={currentDocTitle}
                    onInsertToEditor={(ocrText) => {
                      setLatexFormula((prev) => `${prev}\n\n> **OCR Excerpt:**\n> ${ocrText}`);
                      showToast('OCR text appended to LaTeX note!');
                    }}
                    onLoadNewPdf={(bytes, name) => {
                      setPdfData(bytes);
                      setCurrentDocTitle(name);
                      showToast(`Loaded PDF: ${name}`);
                    }}
                  />
                </div>
              )}

              {/* Right Pane: Live KaTeX Note Editor */}
              {studioSplitMode !== 'pdf' && (
                <div className="studio-pane editor-pane-container">
                  <div className="editor-sub-header">
                    <span className="editor-mode-badge">
                      <Sparkles size={11} style={{ marginRight: 6 }} />
                      LIVE KATEX TYPESETTING
                    </span>
                    <span className="editor-format-help">Supports $inline$ and $$display$$ math</span>
                  </div>

                  <div className="editor-split-body">
                    <textarea
                      className="katex-source-editor"
                      value={latexFormula}
                      onChange={(e) => setLatexFormula(e.target.value)}
                      placeholder="Type Markdown notes and LaTeX formulas ($...$ or $$...$$)..."
                    />

                    {/* Live Rendered Output */}
                    <div className="katex-live-preview-box">
                      <div className="preview-box-header">
                        <span>LIVE RENDERED NOTE</span>
                        <span className="preview-engine-chip">KaTeX 0.19</span>
                      </div>
                      <div className="preview-box-content">
                        <MathRenderer content={latexFormula} />
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Persistent Audio Waveform Scrubber in Studio View */}
            {!isZenMode && (
              <footer className="studio-bottom-audio-bar">
                <button
                  className="btn-audio-play"
                  onClick={() => setIsPlayingAudio(!isPlayingAudio)}
                  title={isPlayingAudio ? 'Pause Audio' : 'Play Audio'}
                >
                  {isPlayingAudio ? <Pause size={13} /> : <Play size={13} style={{ marginLeft: 2 }} />}
                </button>
                <div className="audio-meta-text">
                  <span className="audio-title">Calculus Lecture Audio (Prof. Henderson)</span>
                  <span className="audio-subtitle">02:15 / 05:00 • Anchored to Page 14</span>
                </div>

                <div
                  className="audio-waveform-track"
                  onClick={(e) => {
                    const rect = e.currentTarget.getBoundingClientRect();
                    const x = e.clientX - rect.left;
                    setAudioProgress(Math.round((x / rect.width) * 100));
                  }}
                >
                  <div className="waveform-bars-cluster">
                    {[6, 12, 18, 24, 30, 20, 14, 28, 22, 16, 26, 32, 28, 18, 12, 8, 16, 22, 28, 24, 18, 12, 6, 14, 20, 26, 22, 14, 8, 4].map((h, i) => (
                      <div
                        key={i}
                        className={`audio-bar-stick ${i * 3.3 <= audioProgress ? 'played' : ''}`}
                        style={{ height: `${h}px` }}
                      />
                    ))}
                  </div>
                  <div className="waveform-playhead-line" style={{ left: `${audioProgress}%` }} />
                </div>
              </footer>
            )}
          </div>
        )}

        {/* VIEW 5: SETTINGS VIEW */}
        {activeNav === 'settings' && (
          <div className="settings-page-container">
            <h1 className="settings-main-title">Settings</h1>
            <p className="settings-subtitle">Manage preferences, offline storage, and packages</p>

            <div className="settings-card">
              <h3 className="settings-card-title">
                <Package size={18} style={{ marginRight: 8, verticalAlign: 'middle', color: '#60A5FA' }} />
                Coursework Packages (.fotara)
              </h3>
              <p className="settings-card-desc">
                Drag and drop `.fotara` bundles anywhere onto the window to unpack them offline.
              </p>
              <div className="settings-btn-row">
                <button className="settings-action-btn primary" onClick={handleLoadDemoPackage}>
                  <Sparkles size={14} style={{ marginRight: 6, verticalAlign: 'middle' }} />
                  Load Sample Calculus.fotara Bundle
                </button>
                <button
                  className="settings-action-btn"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <FolderOpen size={14} style={{ marginRight: 6, verticalAlign: 'middle' }} />
                  Import .fotara or .pdf from Disk
                </button>
                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".fotara,.zip,.pdf"
                  style={{ display: 'none' }}
                  onChange={(e) => {
                    if (e.target.files?.[0]) processUploadedFile(e.target.files[0]);
                  }}
                />
              </div>
            </div>

            <div className="settings-card">
              <h3 className="settings-card-title">
                <Command size={18} style={{ marginRight: 8, verticalAlign: 'middle', color: '#F59E0B' }} />
                Hotkeys Reference
              </h3>
              <div className="hotkeys-grid">
                <div className="hotkey-item">
                  <kbd>Ctrl + B</kbd>
                  <span>Toggle Left Sidebar</span>
                </div>
                <div className="hotkey-item">
                  <kbd>Ctrl + K</kbd>
                  <span>Universal Command Palette</span>
                </div>
                <div className="hotkey-item">
                  <kbd>F11 / Ctrl+Shift+Z</kbd>
                  <span>Distraction-Free Zen Mode</span>
                </div>
                <div className="hotkey-item">
                  <kbd>Esc</kbd>
                  <span>Close Palette / Exit Zen Mode</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* ─────────────────────────────────────────────────────────────
          3. FLOATING ZEN MODE EXIT HUD
      ───────────────────────────────────────────────────────────── */}
      {isZenMode && (
        <div className="floating-zen-hud">
          <Maximize2 size={13} style={{ marginRight: 6, color: '#60A5FA' }} />
          <span>Zen Mode Active</span>
          <button className="btn-exit-zen" onClick={() => setIsZenMode(false)}>
            <X size={12} style={{ marginRight: 4, verticalAlign: 'middle' }} />
            Exit (Esc)
          </button>
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────
          4. DRAG AND DROP FULLSCREEN OVERLAY
      ───────────────────────────────────────────────────────────── */}
      {isDraggingOver && (
        <div className="fotara-drag-overlay">
          <div className="drag-overlay-card">
            <Upload size={48} className="drag-upload-icon" />
            <h2 className="drag-overlay-title">Drop Coursework Bundle or PDF Here</h2>
            <p className="drag-overlay-subtitle">
              Drop `.fotara` archive or `.pdf` lecture notes to unpack directly into Course Explorer.
            </p>
          </div>
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────
          5. TOAST NOTIFICATIONS
      ───────────────────────────────────────────────────────────── */}
      {toastMessage && (
        <div className="fotara-toast-notification">
          <span>{toastMessage}</span>
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────
          6. CREATE NEW FOLDER MODAL
      ───────────────────────────────────────────────────────────── */}
      {isNewFolderModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsNewFolderModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Create New Folder</h3>
              <button
                className="btn-modal-close"
                onClick={() => setIsNewFolderModalOpen(false)}
              >
                <X size={16} />
              </button>
            </div>
            <div className="modal-body">
              <label className="modal-label">Folder Name</label>
              <input
                autoFocus
                type="text"
                className="modal-input"
                placeholder="e.g. Kimia Dasar, Biologi Sel..."
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleCreateFolder();
                }}
              />

              <label className="modal-label" style={{ marginTop: '14px' }}>
                Accent Color
              </label>
              <div className="color-picker-row">
                {['#EF4444', '#3B82F6', '#A855F7', '#F59E0B', '#8B5CF6', '#10B981'].map((c) => (
                  <div
                    key={c}
                    className={`color-tile-choice ${newFolderColor === c ? 'selected' : ''}`}
                    style={{ backgroundColor: c }}
                    onClick={() => setNewFolderColor(c)}
                  />
                ))}
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn-modal-cancel"
                onClick={() => setIsNewFolderModalOpen(false)}
              >
                Cancel
              </button>
              <button
                className="btn-modal-confirm"
                onClick={handleCreateFolder}
                disabled={!newFolderName.trim()}
              >
                Create Folder
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────
          7. UNIVERSAL COMMAND PALETTE MODAL (Ctrl + K)
      ───────────────────────────────────────────────────────────── */}
      {isCommandPaletteOpen && (
        <div className="palette-backdrop" onClick={() => setIsCommandPaletteOpen(false)}>
          <div className="palette-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="palette-search-row">
              <Search size={16} className="palette-search-glyph" />
              <input
                autoFocus
                type="text"
                className="palette-search-field"
                placeholder="Type a command or search notes (e.g. 'Jadwal', 'Zen', 'Sample')..."
              />
              <kbd className="kbd-subtle" onClick={() => setIsCommandPaletteOpen(false)}>
                ESC
              </kbd>
            </div>
            <div className="palette-results">
              <div className="palette-section-header">QUICK ACTIONS</div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setActiveNav('home');
                  setIsCommandPaletteOpen(false);
                }}
              >
                <Home size={15} className="item-glyph" />
                <span className="item-text">Jump to Home Dashboard</span>
              </div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setActiveNav('notes');
                  setIsCommandPaletteOpen(false);
                }}
              >
                <FileText size={15} className="item-glyph" />
                <span className="item-text">Open Unified Notes Screen</span>
              </div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setActiveNav('studio');
                  setIsCommandPaletteOpen(false);
                }}
              >
                <BookOpen size={15} className="item-glyph" />
                <span className="item-text">Open Widescreen Study Studio</span>
              </div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setIsZenMode(!isZenMode);
                  setIsCommandPaletteOpen(false);
                }}
              >
                <Maximize2 size={15} className="item-glyph" />
                <span className="item-text">Toggle Focus Zen Mode</span>
                <span className="item-shortcut">F11</span>
              </div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setIsSidebarOpen(!isSidebarOpen);
                  setIsCommandPaletteOpen(false);
                }}
              >
                <LayoutGrid size={15} className="item-glyph" />
                <span className="item-text">Toggle Left Sidebar</span>
                <span className="item-shortcut">Ctrl+B</span>
              </div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setIsCommandPaletteOpen(false);
                  handleLoadDemoPackage();
                }}
              >
                <Sparkles size={15} className="item-glyph" />
                <span className="item-text">Load Sample Calculus.fotara Bundle</span>
              </div>

              <div
                className="palette-result-item"
                onClick={() => {
                  setIsCommandPaletteOpen(false);
                  setIsNewFolderModalOpen(true);
                }}
              >
                <Plus size={15} className="item-glyph" />
                <span className="item-text">Create New Folder</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;
