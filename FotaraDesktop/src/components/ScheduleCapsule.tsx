// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import React from 'react';
import { ChevronRight } from 'lucide-react';

interface ScheduleCapsuleProps {
  courseName?: string;
  room?: string;
  timeRange?: string;
  timeRemaining?: string;
  isTomorrow?: boolean;
  onClick?: () => void;
}

/**
 * 1-Line Dynamic Schedule Capsule matching Android Phase 42 specification:
 * - Height: 36px pill (CircleShape ends)
 * - Muted card background with subtle border
 * - Status dot (Green for today, Amber for tomorrow rollover)
 * - Course title + room + time remaining countdown
 */
export const ScheduleCapsule: React.FC<ScheduleCapsuleProps> = ({
  courseName = 'Kalkulus II',
  room = 'R. 302',
  timeRange = '08:00 - 09:40',
  timeRemaining = '45m left',
  isTomorrow = false,
  onClick,
}) => {
  return (
    <div className="schedule-capsule-pill" onClick={onClick} title="Class Timetable & Agenda">
      <div className="capsule-live-dot-wrap">
        <span className={`capsule-live-dot ${isTomorrow ? 'rollover' : 'active'}`} />
      </div>

      <div className="capsule-text-line">
        <span className="capsule-course-title">{courseName}</span>
        <span className="capsule-room-tag">({room})</span>
        <span className="capsule-meta-divider">•</span>
        <span className="capsule-time-text">{timeRange}</span>
        <span className="capsule-meta-divider">•</span>
        <span className="capsule-countdown-badge">{timeRemaining}</span>
      </div>

      <div className="capsule-arrow-icon">
        <ChevronRight size={14} />
      </div>
    </div>
  );
};
