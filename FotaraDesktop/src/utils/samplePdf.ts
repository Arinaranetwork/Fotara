// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

/**
 * Generates a valid multi-page PDF 1.4 document buffer for academic coursework demonstration.
 * Complies strictly with ISO 32000-1 (PDF) specification with exact byte offsets.
 */
export function generateAcademicLecturePdf(): Uint8Array {
  const pages = [
    {
      title: 'Calculus II: Chapter 4 - Fourier Series',
      subtitle: 'Lecture 04 • Prof. Henderson • Dept of Mathematics',
      body: [
        'Theorem 4.2 (Dirichlet Boundary Conditions):',
        'Let f(x) be a piecewise smooth function defined on the interval [-L, L].',
        'Under Dirichlet conditions, the full Fourier series representation converges point-wise:',
        'f(x) = a0 / 2 + Sum_n=1..inf [ an * cos(n*pi*x / L) + bn * sin(n*pi*x / L) ]',
        'Where sinusoidal harmonics form an orthogonal basis in the L2 Hilbert space.',
        'Extracted OCR Confidence: 100% on-device neural parser.'
      ]
    },
    {
      title: 'Calculus II: Orthogonality & Coefficients',
      subtitle: 'Lecture 04 (Part 2) • Orthogonal Basis Functions',
      body: [
        'Evaluation of Fourier Coefficients via Inner Products:',
        'Integral from -L to L of cos(n*pi*x/L)*cos(m*pi*x/L) dx = L * delta_nm',
        'Integral from -L to L of sin(n*pi*x/L)*sin(m*pi*x/L) dx = L * delta_nm',
        'Integral from -L to L of sin(n*pi*x/L)*cos(m*pi*x/L) dx = 0',
        'Therefore: an = (1/L) * Integral[-L, L] f(x)*cos(n*pi*x/L) dx',
        'And:       bn = (1/L) * Integral[-L, L] f(x)*sin(n*pi*x/L) dx'
      ]
    },
    {
      title: 'Calculus II: Applications to Heat Equation',
      subtitle: 'Lecture 04 (Part 3) • 1D Heat Conduction & PDEs',
      body: [
        'Application: One-Dimensional Heat Conduction in a Rod:',
        'Partial Differential Equation: du/dt = alpha^2 * (d^2 u / dx^2)',
        'Boundary Conditions: u(0, t) = 0 and u(L, t) = 0 for all t >= 0',
        'Initial Condition: u(x, 0) = f(x)',
        'General Solution via Separation of Variables:',
        'u(x, t) = Sum_n=1..inf bn * sin(n*pi*x / L) * exp(-alpha^2 * (n*pi/L)^2 * t)'
      ]
    }
  ];

  const lines: string[] = [];
  lines.push('%PDF-1.4');
  lines.push('%âãÏÓ'); // 4 binary characters > 127 for binary PDF flag

  // Objects to emit
  // 1 0 obj: Catalog
  // 2 0 obj: Pages
  // 3 0 obj: Font Helvetica
  // 4, 5, 6: Page 1, 2, 3
  // 7, 8, 9: Content streams 1, 2, 3

  const offsets: number[] = [0]; // offset for obj 0

  function buildStreamContent(page: typeof pages[0]): string {
    const streamCommands: string[] = [];
    streamCommands.push('BT');
    streamCommands.push('/F1 18 Tf');
    streamCommands.push('50 720 Td');
    streamCommands.push(`(${escapePdfString(page.title)}) Tj`);
    streamCommands.push('/F1 11 Tf');
    streamCommands.push('0 -28 Td');
    streamCommands.push(`(${escapePdfString(page.subtitle)}) Tj`);
    streamCommands.push('0 -35 Td');

    page.body.forEach((b) => {
      streamCommands.push(`(${escapePdfString(b)}) Tj`);
      streamCommands.push('0 -22 Td');
    });

    streamCommands.push('ET');
    return streamCommands.join('\n');
  }

  function escapePdfString(str: string): string {
    return str.replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)');
  }

  // Generate content streams
  const streams = pages.map((p) => buildStreamContent(p));

  // Build the full PDF text with tracking offsets
  let pdfText = lines.join('\n') + '\n';

  function appendObj(objNum: number, content: string) {
    const offset = new TextEncoder().encode(pdfText).length;
    offsets[objNum] = offset;
    pdfText += `${objNum} 0 obj\n${content}\nendobj\n`;
  }

  // 1: Catalog
  appendObj(1, '<< /Type /Catalog /Pages 2 0 R >>');

  // 2: Pages
  appendObj(2, '<< /Type /Pages /Kids [4 0 R 5 0 R 6 0 R] /Count 3 >>');

  // 3: Font
  appendObj(3, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');

  // 4, 5, 6: Pages
  for (let i = 0; i < 3; i++) {
    const pageObjNum = 4 + i;
    const contentObjNum = 7 + i;
    appendObj(
      pageObjNum,
      `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents ${contentObjNum} 0 R >>`
    );
  }

  // 7, 8, 9: Content streams
  for (let i = 0; i < 3; i++) {
    const streamObjNum = 7 + i;
    const stream = streams[i];
    const streamLength = new TextEncoder().encode(stream).length;
    appendObj(
      streamObjNum,
      `<< /Length ${streamLength} >>\nstream\n${stream}\nendstream`
    );
  }

  // xref table
  const startXref = new TextEncoder().encode(pdfText).length;
  pdfText += 'xref\n';
  pdfText += `0 10\n`;
  pdfText += '0000000000 65535 f \n';
  for (let i = 1; i <= 9; i++) {
    const off = offsets[i].toString().padStart(10, '0');
    pdfText += `${off} 00000 n \n`;
  }

  // trailer
  pdfText += 'trailer\n';
  pdfText += '<< /Size 10 /Root 1 0 R >>\n';
  pdfText += 'startxref\n';
  pdfText += `${startXref}\n`;
  pdfText += '%%EOF\n';

  return new TextEncoder().encode(pdfText);
}
