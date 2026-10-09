// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

import React, { useMemo } from 'react';
import katex from 'katex';
import 'katex/dist/katex.min.css';

interface MathRendererProps {
  content: string;
  className?: string;
}

/**
 * Parses text containing inline ($...$) and display ($$...$$) LaTeX expressions
 * and renders them safely using KaTeX.
 */
export const MathRenderer: React.FC<MathRendererProps> = ({ content, className = '' }) => {
  const renderedContent = useMemo(() => {
    if (!content) return '';

    // Regex to split text by display math ($$...$$) and inline math ($...$)
    // Match $$...$$ first, then $...$
    const mathRegex = /(\$\$[\s\S]*?\$\$|\$[^\$\n]+?\$)/g;
    const parts = content.split(mathRegex);

    return parts.map((part, index) => {
      if (part.startsWith('$$') && part.endsWith('$$') && part.length >= 4) {
        const formula = part.slice(2, -2).trim();
        try {
          const html = katex.renderToString(formula, {
            displayMode: true,
            throwOnError: false,
            strict: false,
          });
          return (
            <div
              key={`display-${index}`}
              className="katex-display-wrapper"
              dangerouslySetInnerHTML={{ __html: html }}
            />
          );
        } catch (err) {
          return (
            <div key={`err-${index}`} className="katex-error">
              <code>{part}</code>
            </div>
          );
        }
      } else if (part.startsWith('$') && part.endsWith('$') && part.length >= 2) {
        const formula = part.slice(1, -1).trim();
        try {
          const html = katex.renderToString(formula, {
            displayMode: false,
            throwOnError: false,
            strict: false,
          });
          return (
            <span
              key={`inline-${index}`}
              className="katex-inline-wrapper"
              dangerouslySetInnerHTML={{ __html: html }}
            />
          );
        } catch (err) {
          return (
            <span key={`err-${index}`} className="katex-error">
              <code>{part}</code>
            </span>
          );
        }
      }

      // Format basic markdown lines (headers, bullet points, linebreaks)
      return (
        <span key={`text-${index}`} className="math-text-segment">
          {renderTextWithNewlines(part)}
        </span>
      );
    });
  }, [content]);

  return <div className={`math-renderer ${className}`}>{renderedContent}</div>;
};

/**
 * Helper to preserve linebreaks and basic Markdown formatting in text segments
 */
function renderTextWithNewlines(text: string): React.ReactNode {
  const lines = text.split('\n');
  return lines.map((line, lIdx) => {
    let formatted: React.ReactNode = line;

    if (line.startsWith('### ')) {
      formatted = <h4 className="note-subheading-h3">{line.slice(4)}</h4>;
    } else if (line.startsWith('## ')) {
      formatted = <h3 className="note-subheading-h2">{line.slice(3)}</h3>;
    } else if (line.startsWith('# ')) {
      formatted = <h2 className="note-heading-h1">{line.slice(2)}</h2>;
    } else if (line.startsWith('- ') || line.startsWith('* ')) {
      formatted = <div className="note-bullet-line">• {line.slice(2)}</div>;
    }

    return (
      <React.Fragment key={lIdx}>
        {formatted}
        {lIdx < lines.length - 1 && <br />}
      </React.Fragment>
    );
  });
}

/**
 * Dedicated single formula KaTeX block
 */
export const KatexFormulaBlock: React.FC<{ formula: string; displayMode?: boolean }> = ({
  formula,
  displayMode = true,
}) => {
  const renderedHtml = useMemo(() => {
    try {
      return katex.renderToString(formula || '', {
        displayMode,
        throwOnError: false,
        strict: false,
      });
    } catch (err) {
      return `<span class="katex-error">${formula}</span>`;
    }
  }, [formula, displayMode]);

  return (
    <div
      className={displayMode ? 'katex-display-block' : 'katex-inline-block'}
      dangerouslySetInnerHTML={{ __html: renderedHtml }}
    />
  );
};
