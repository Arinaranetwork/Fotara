// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import React, { useState } from 'react';
import {
  ChevronLeft,
  Folder,
  Plus,
  Upload,
  PenTool,
  BookOpen,
  MoreVertical,
  Link,
} from 'lucide-react';
import type { UnifiedDesktopNote } from './NotesListView';

interface FolderDetailViewProps {
  folderName: string;
  folderColor: string;
  notes: UnifiedDesktopNote[];
  isLinked?: boolean;
  linkedFolderName?: string;
  onBack: () => void;
  onOpenNote: (note: UnifiedDesktopNote) => void;
  onOpenInStudio: () => void;
  onAddNote: () => void;
}

export const FolderDetailView: React.FC<FolderDetailViewProps> = ({
  folderName,
  folderColor,
  notes,
  isLinked = false,
  linkedFolderName,
  onBack,
  onOpenNote,
  onOpenInStudio,
  onAddNote,
}) => {
  const [activeSubfolderTab, setActiveSubfolderTab] = useState<'all' | 'lectures' | 'assignments'>('all');

  const folderNotes = notes.filter((n) => n.folder.toLowerCase() === folderName.toLowerCase());

  const isLectureNote = (note: UnifiedDesktopNote) => {
    return (
      note.subfolder === 'lectures' ||
      note.type === 'pdf' ||
      note.type === 'canvas' ||
      note.title.toLowerCase().includes('canvas') ||
      note.title.toLowerCase().includes('lecture') ||
      note.title.toLowerCase().includes('catatan') ||
      note.title.toLowerCase().includes('materi')
    );
  };

  const isAssignmentNote = (note: UnifiedDesktopNote) => {
    return (
      note.subfolder === 'assignments' ||
      note.type === 'docx' ||
      note.type === 'txt' ||
      note.title.toLowerCase().includes('kisi') ||
      note.title.toLowerCase().includes('tugas') ||
      note.title.toLowerCase().includes('prep') ||
      note.title.toLowerCase().includes('test') ||
      note.title.toLowerCase().includes('kuis')
    );
  };

  const lectureNotes = folderNotes.filter(isLectureNote);
  const assignmentNotes = folderNotes.filter(isAssignmentNote);

  const displayedNotes = folderNotes.filter((note) => {
    if (activeSubfolderTab === 'all') return true;
    if (activeSubfolderTab === 'lectures') return isLectureNote(note);
    if (activeSubfolderTab === 'assignments') return isAssignmentNote(note);
    return true;
  });

  return (
    <div className="folder-detail-container">
      {/* Top Breadcrumb Bar */}
      <div className="folder-detail-top-bar">
        <button className="btn-detail-back" onClick={onBack}>
          <ChevronLeft size={16} />
          <span>Home</span>
        </button>
        <span className="detail-breadcrumb-sep">/</span>
        <span className="detail-breadcrumb-current">{folderName}</span>

        <div className="detail-top-actions">
          <button className="btn-open-in-studio" onClick={onOpenInStudio} title="Open Split Study Studio">
            <BookOpen size={15} />
            <span>Open in Studio</span>
          </button>
        </div>
      </div>

      {/* Folder Header Hero */}
      <div className="folder-detail-hero">
        <div className="folder-hero-left">
          <div
            className="folder-hero-accent-tile"
            style={{
              backgroundColor: `${folderColor}26`,
              borderColor: `${folderColor}55`,
              color: folderColor,
            }}
          >
            <Folder size={32} />
          </div>
          <div className="folder-hero-titles">
            <h1 className="folder-hero-name">{folderName}</h1>
            <div className="folder-hero-meta-row">
              <span className="folder-hero-notes-count">
                {folderNotes.length} {folderNotes.length === 1 ? 'coursework item' : 'coursework items'}
              </span>
              {isLinked && (
                <span className="folder-linked-chip" title="Linked via LinkIt cluster">
                  <Link size={12} />
                  <span>Linked with {linkedFolderName || 'Folder'}</span>
                </span>
              )}
            </div>
          </div>
        </div>

        <div className="folder-hero-action-buttons">
          <button className="btn-hero-action primary" onClick={onAddNote}>
            <Plus size={16} />
            <span>Add Note</span>
          </button>
          <button className="btn-hero-action" onClick={onOpenInStudio}>
            <Upload size={15} />
            <span>Import PDF</span>
          </button>
        </div>
      </div>

      {/* Subfolder Tabs (All, Lectures, Assignments) */}
      <div className="folder-subfolder-tabs">
        <button
          className={`subfolder-tab-btn ${activeSubfolderTab === 'all' ? 'active' : ''}`}
          onClick={() => setActiveSubfolderTab('all')}
        >
          All Items ({folderNotes.length})
        </button>
        <button
          className={`subfolder-tab-btn ${activeSubfolderTab === 'lectures' ? 'active' : ''}`}
          onClick={() => setActiveSubfolderTab('lectures')}
        >
          Lectures ({lectureNotes.length})
        </button>
        <button
          className={`subfolder-tab-btn ${activeSubfolderTab === 'assignments' ? 'active' : ''}`}
          onClick={() => setActiveSubfolderTab('assignments')}
        >
          Assignments & Prep ({assignmentNotes.length})
        </button>
      </div>

      {/* Notes Grid inside Folder */}
      <div className="folder-notes-grid">
        {displayedNotes.map((note) => (
          <div key={note.id} className="folder-item-card" onClick={() => onOpenNote(note)}>
            <div className="folder-item-top">
              <div className={`folder-item-badge ${note.type}-badge`}>
                {note.type === 'canvas' && <PenTool size={16} />}
                {note.type === 'pdf' && <span className="mini-tag">PDF</span>}
                {note.type === 'docx' && <span className="mini-tag">DOCX</span>}
                {note.type === 'txt' && <span className="mini-tag">TXT</span>}
              </div>
              <button
                className="btn-card-dots"
                onClick={(e) => {
                  e.stopPropagation();
                }}
              >
                <MoreVertical size={15} />
              </button>
            </div>

            <div className="folder-item-info">
              <h4 className="folder-item-title">{note.title}</h4>
              <p className="folder-item-timestamp">{note.timestamp}</p>
            </div>
          </div>
        ))}

        {displayedNotes.length === 0 && (
          <div className="folder-empty-notice">
            <p>No coursework items in this section. Drop a .fotara package or click Add Note to begin.</p>
          </div>
        )}
      </div>
    </div>
  );
};
