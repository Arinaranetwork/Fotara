// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import React, { useState } from 'react';
import {
  Search,
  MoreVertical,
  Folder,
  PenTool,
  Plus,
  Image,
  FileText,
  Layers,
} from 'lucide-react';
import { ScheduleCapsule } from './ScheduleCapsule';

export type NoteCategoryFilter = 'all' | 'photos' | 'documents' | 'text' | 'canvas';

export interface UnifiedDesktopNote {
  id: string;
  title: string;
  type: 'canvas' | 'txt' | 'docx' | 'pdf' | 'photo';
  folder: string;
  timestamp: string;
  dateGroup: 'today' | 'yesterday' | 'earlier';
  content?: string;
  thumbnailUrl?: string;
  subfolder?: 'lectures' | 'assignments';
}

interface NotesListViewProps {
  notes: UnifiedDesktopNote[];
  onOpenNote: (note: UnifiedDesktopNote) => void;
  onOpenFolder: (folderName: string) => void;
  onCreateNote: () => void;
}

export const NotesListView: React.FC<NotesListViewProps> = ({
  notes,
  onOpenNote,
  onOpenFolder,
  onCreateNote,
}) => {
  const [selectedFilter, setSelectedFilter] = useState<NoteCategoryFilter>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Filter notes by category and search query
  const filteredNotes = notes.filter((note) => {
    // Filter chip matching
    if (selectedFilter === 'photos' && note.type !== 'photo') return false;
    if (selectedFilter === 'documents' && note.type !== 'pdf' && note.type !== 'docx') return false;
    if (selectedFilter === 'text' && note.type !== 'txt') return false;
    if (selectedFilter === 'canvas' && note.type !== 'canvas') return false;

    // Search query matching
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        note.title.toLowerCase().includes(q) ||
        note.folder.toLowerCase().includes(q) ||
        (note.content && note.content.toLowerCase().includes(q))
      );
    }
    return true;
  });

  const todayNotes = filteredNotes.filter((n) => n.dateGroup === 'today');
  const yesterdayNotes = filteredNotes.filter((n) => n.dateGroup === 'yesterday');
  const earlierNotes = filteredNotes.filter((n) => n.dateGroup === 'earlier');

  return (
    <div className="notes-screen-root">
      {/* Top Header */}
      <div className="notes-screen-header">
        <div className="notes-header-left">
          <h1 className="notes-main-title">Notes</h1>
          <p className="notes-tagline">All notes, unified across coursework</p>
        </div>

        <div className="notes-header-actions">
          <div className="notes-search-pill">
            <Search size={15} className="notes-search-glyph" />
            <input
              type="text"
              placeholder="Search by keywords, OCR text, or title..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="notes-search-input"
            />
          </div>

          <button className="btn-notes-new" onClick={onCreateNote} title="Create New Note">
            <Plus size={18} />
            <span>New Note</span>
          </button>
        </div>
      </div>

      {/* 1-Line Dynamic Schedule Capsule (Android Phase 42 Integration) */}
      <div className="notes-schedule-wrapper">
        <ScheduleCapsule
          courseName="Kalkulus II"
          room="R. 302"
          timeRange="08:00 - 09:40"
          timeRemaining="45m left"
        />
      </div>

      {/* Horizontal Filter Chips (Matching Android NoteFilterChip) */}
      <div className="notes-filter-chips-row">
        <button
          className={`filter-pill-chip ${selectedFilter === 'all' ? 'active' : ''}`}
          onClick={() => setSelectedFilter('all')}
        >
          All Notes
        </button>
        <button
          className={`filter-pill-chip ${selectedFilter === 'photos' ? 'active' : ''}`}
          onClick={() => setSelectedFilter('photos')}
        >
          <Image size={13} />
          <span>Photos</span>
        </button>
        <button
          className={`filter-pill-chip ${selectedFilter === 'documents' ? 'active' : ''}`}
          onClick={() => setSelectedFilter('documents')}
        >
          <FileText size={13} />
          <span>Documents (PDF / DOCX)</span>
        </button>
        <button
          className={`filter-pill-chip ${selectedFilter === 'text' ? 'active' : ''}`}
          onClick={() => setSelectedFilter('text')}
        >
          <PenTool size={13} />
          <span>Text Notes (LaTeX)</span>
        </button>
        <button
          className={`filter-pill-chip ${selectedFilter === 'canvas' ? 'active' : ''}`}
          onClick={() => setSelectedFilter('canvas')}
        >
          <Layers size={13} />
          <span>Vector Canvas</span>
        </button>
      </div>

      {/* Date Grouped Notes List */}
      <div className="notes-date-groups-container">
        {todayNotes.length > 0 && (
          <div className="date-group-block">
            <div className="date-group-header">
              <span className="date-dot dot-today" />
              <span className="date-label">TODAY</span>
              <span className="date-count">{todayNotes.length}</span>
            </div>
            <div className="date-group-items">
              {todayNotes.map((note) => (
                <NoteCardRow
                  key={note.id}
                  note={note}
                  onOpenNote={onOpenNote}
                  onOpenFolder={onOpenFolder}
                />
              ))}
            </div>
          </div>
        )}

        {yesterdayNotes.length > 0 && (
          <div className="date-group-block">
            <div className="date-group-header">
              <span className="date-dot dot-yesterday" />
              <span className="date-label">YESTERDAY</span>
              <span className="date-count">{yesterdayNotes.length}</span>
            </div>
            <div className="date-group-items">
              {yesterdayNotes.map((note) => (
                <NoteCardRow
                  key={note.id}
                  note={note}
                  onOpenNote={onOpenNote}
                  onOpenFolder={onOpenFolder}
                />
              ))}
            </div>
          </div>
        )}

        {earlierNotes.length > 0 && (
          <div className="date-group-block">
            <div className="date-group-header">
              <span className="date-dot dot-earlier" />
              <span className="date-label">EARLIER THIS WEEK</span>
              <span className="date-count">{earlierNotes.length}</span>
            </div>
            <div className="date-group-items">
              {earlierNotes.map((note) => (
                <NoteCardRow
                  key={note.id}
                  note={note}
                  onOpenNote={onOpenNote}
                  onOpenFolder={onOpenFolder}
                />
              ))}
            </div>
          </div>
        )}

        {filteredNotes.length === 0 && (
          <div className="notes-empty-state">
            <p>No notes matching your current filter.</p>
          </div>
        )}
      </div>
    </div>
  );
};

const NoteCardRow: React.FC<{
  note: UnifiedDesktopNote;
  onOpenNote: (n: UnifiedDesktopNote) => void;
  onOpenFolder: (f: string) => void;
}> = ({ note, onOpenNote, onOpenFolder }) => {
  return (
    <div className="unified-note-card-row" onClick={() => onOpenNote(note)}>
      <div className="note-type-cell">
        {note.type === 'canvas' && (
          <div className="note-badge-tile canvas-tile" title="Vector Drawing Canvas">
            <PenTool size={16} />
          </div>
        )}
        {note.type === 'photo' && (
          <div className="note-badge-tile photo-tile" title="Captured Coursework Photo">
            <Image size={16} />
          </div>
        )}
        {note.type === 'pdf' && (
          <div className="note-badge-tile pdf-tile" title="PDF Document">
            <span className="tile-text-tag">PDF</span>
          </div>
        )}
        {note.type === 'docx' && (
          <div className="note-badge-tile docx-tile" title="Word Document">
            <span className="tile-text-tag">DOCX</span>
          </div>
        )}
        {note.type === 'txt' && (
          <div className="note-badge-tile txt-tile" title="LaTeX Markdown Note">
            <span className="tile-text-tag">TXT</span>
          </div>
        )}

        <div className="note-title-column">
          <span className="note-main-title">{note.title}</span>
          {note.content && <span className="note-preview-snippet">{note.content.slice(0, 75)}...</span>}
        </div>
      </div>

      <div
        className="note-folder-tag"
        onClick={(e) => {
          e.stopPropagation();
          onOpenFolder(note.folder);
        }}
        title={`View folder: ${note.folder}`}
      >
        <Folder size={14} className="tag-folder-icon" />
        <span>{note.folder}</span>
      </div>

      <div className="note-time-cell">
        <span>{note.timestamp}</span>
      </div>

      <div className="note-menu-cell">
        <button
          className="btn-note-dots"
          onClick={(e) => {
            e.stopPropagation();
          }}
        >
          <MoreVertical size={16} />
        </button>
      </div>
    </div>
  );
};
