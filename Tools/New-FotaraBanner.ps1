# --- Arinara Network (c) 2026 ---
# Exclusive property of Arinara Network.
# Unauthorized use, reproduction, distribution, or modification of this code,
# in whole or in part, for any purpose, is strictly prohibited without prior
# written consent from Arinara Network as sole legal owner of this codebase.

param(
    [string]$Version = "2.6.2",
    [string]$Codename = "Hyperion",
    [string]$Description = "Telegram-Style App Icon Carousel, 6x Animated GIF Folder Covers, Custom Folder Icon Badges, and Edge-to-Edge Schedule Settings",
    [string]$ReleaseDate = "2026-11-15",
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$TemplatePath = Join-Path $ProjectRoot "Assets\Banners\Banner Template.jpg"
if (-not (Test-Path $TemplatePath)) {
    $TemplatePath = Join-Path $ProjectRoot "Assets\Banners\Template New Banner (1).jpg"
}
$ExamplePath  = Join-Path $ProjectRoot "Assets\Banners\Banner Example.jpg"
$FontBoldPath = Join-Path $ProjectRoot "app\src\main\res\font\elms_sans_bold.ttf"
$FontMedPath  = Join-Path $ProjectRoot "app\src\main\res\font\elms_sans_medium.ttf"
$FontRegPath  = Join-Path $ProjectRoot "app\src\main\res\font\elms_sans_regular.ttf"

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $ProjectRoot "Assets\Banners\FotaraBanner_${Version}_${ReleaseDate}.png"
}

if (-not (Test-Path $TemplatePath)) { throw "Missing template: $TemplatePath" }
if (-not (Test-Path $FontBoldPath)) { throw "Missing ElmsSans Bold font: $FontBoldPath" }

$baseImg = [System.Drawing.Bitmap]::new($TemplatePath)
$W = $baseImg.Width
$H = $baseImg.Height

$bullet = [char]0x2022
$deg    = [char]0x00B0

# Load bundled ElmsSans fonts
$pfc = [System.Drawing.Text.PrivateFontCollection]::new()
$pfc.AddFontFile($FontBoldPath)
if (Test-Path $FontMedPath) { $pfc.AddFontFile($FontMedPath) }
if (Test-Path $FontRegPath) { $pfc.AddFontFile($FontRegPath) }
$elmsFamily = $pfc.Families[0]

# Snapshot top-left pre-baked logo region (X=192..302, Y=258..302 on 2460x1000) to guarantee 0% modification
$logoGuardRect = [System.Drawing.Rectangle]::new([int]($W * 0.072), [int]($H * 0.24), [int]($W * 0.065), [int]($H * 0.08))
$logoGuardSnapshot = $baseImg.Clone($logoGuardRect, $baseImg.PixelFormat)

$canvas = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

# 1. Draw untouched base template (preserves top-left logos and soft blue bottom gradient)
$g.DrawImage($baseImg, 0, 0, $W, $H)

function New-RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $maxR = [Math]::Min($w, $h) / 2.0
    $safeR = [Math]::Min($r, $maxR)
    $d = [float]($safeR * 2.0)
    if ($d -le 1.0) {
        $path.AddRectangle([System.Drawing.RectangleF]::new($x, $y, $w, $h))
        return $path
    }
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-TopRoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $maxR = [Math]::Min($w, $h) / 2.0
    $safeR = [Math]::Min($r, $maxR)
    $d = [float]($safeR * 2.0)
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddLine($x + $w, $y + $safeR, $x + $w, $y + $h)
    $path.AddLine($x + $w, $y + $h, $x, $y + $h)
    $path.AddLine($x, $y + $h, $x, $y + $safeR)
    $path.CloseFigure()
    return $path
}

# Electric Slate Dark Theme Palette
$cBgCanvas       = [System.Drawing.Color]::FromArgb(255, 13, 17, 23)     # #0D1117
$cSurfacePanel   = [System.Drawing.Color]::FromArgb(255, 22, 28, 38)     # #161C26
$cSurfaceElev    = [System.Drawing.Color]::FromArgb(255, 31, 39, 54)     # #1F2736
$cBorderSubtle   = [System.Drawing.Color]::FromArgb(255, 38, 47, 62)     # #262F3E
$cTrackInactive  = [System.Drawing.Color]::FromArgb(255, 40, 50, 69)     # #283245
$cTextPrimary    = [System.Drawing.Color]::FromArgb(255, 248, 250, 252)  # #F8FAFC
$cTextMuted      = [System.Drawing.Color]::FromArgb(255, 148, 163, 184)  # #94A3B8
$cPrimaryAccent  = [System.Drawing.Color]::FromArgb(255, 122, 90, 248)   # #7A5AF8
$cAccentSoft     = [System.Drawing.Color]::FromArgb(48, 122, 90, 248)
$cEmerald        = [System.Drawing.Color]::FromArgb(255, 16, 185, 129)

$bBgCanvas      = [System.Drawing.SolidBrush]::new($cBgCanvas)
$bSurfacePanel  = [System.Drawing.SolidBrush]::new($cSurfacePanel)
$bSurfaceElev   = [System.Drawing.SolidBrush]::new($cSurfaceElev)
$bTrackInactive = [System.Drawing.SolidBrush]::new($cTrackInactive)
$bTextPrimary   = [System.Drawing.SolidBrush]::new($cTextPrimary)
$bTextMuted     = [System.Drawing.SolidBrush]::new($cTextMuted)
$bPrimaryAccent = [System.Drawing.SolidBrush]::new($cPrimaryAccent)
$bAccentSoft    = [System.Drawing.SolidBrush]::new($cAccentSoft)
$bEmerald       = [System.Drawing.SolidBrush]::new($cEmerald)
$pBorderSubtle  = [System.Drawing.Pen]::new($cBorderSubtle, 2.0)
$pAccentBorder  = [System.Drawing.Pen]::new($cPrimaryAccent, 2.5)

# Exact slot coordinates measured from updated Assets\Banners\Banner Example.jpg (2460x1000)
# Left Slot (Brown rectangle: X=1266..1686, Y=274..1000 — Back/Mid Layer, anchored to bottom edge)
$slot1X = [float][Math]::Round($W * (1266.0 / 2460.0))
$slot1Y = [float][Math]::Round($H * (274.0  / 1000.0))
$slot1W = [float][Math]::Round($W * (420.0  / 2460.0))
$slot1H = [float]($H - $slot1Y)

# Far-Right Slot (Light-Blue rectangle: X=1872..2336, Y=192..1000 — Tallest Layer, anchored to bottom edge)
$slot3X = [float][Math]::Round($W * (1872.0 / 2460.0))
$slot3Y = [float][Math]::Round($H * (192.0  / 1000.0))
$slot3W = [float][Math]::Round($W * (464.0  / 2460.0))
$slot3H = [float]($H - $slot3Y)

# Center-Right Slot (Dark-Gray rectangle: X=1554..1866, Y=400..1000 — Foreground Layer overlapping Slot 1 & Slot 3)
$slot2X = [float][Math]::Round($W * (1554.0 / 2460.0))
$slot2Y = [float][Math]::Round($H * (400.0  / 1000.0))
$slot2W = [float][Math]::Round($W * (312.0  / 2460.0))
$slot2H = [float]($H - $slot2Y)

$fCardHeader = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0085), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fCardSub    = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0060), [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$fCardLabel  = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0052), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fCardBody   = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0064), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)

# ==============================================================================
# 2A. Render Slot 1 (Back/Mid Layer): Appearance — Telegram-Style App Icon Carousel & 3x/Day Schedule
# ==============================================================================
$pathSlot1 = New-TopRoundedRectPath $slot1X $slot1Y $slot1W $slot1H 22.0
$state1 = $g.Save()
$g.SetClip($pathSlot1)
$g.FillPath($bBgCanvas, $pathSlot1)

# Top violet accent bar
$g.FillRectangle($bTrackInactive, $slot1X, $slot1Y, $slot1W, 6.0)
$g.FillRectangle($bPrimaryAccent, $slot1X, $slot1Y, [float]($slot1W * 0.78), 6.0)

$pad1 = [float]($slot1W * 0.065)
$curY1 = $slot1Y + 18.0

$g.DrawString("SETTINGS $bullet APPEARANCE", $fCardLabel, $bPrimaryAccent, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0072)
$g.DrawString("App Icon Showcase", $fCardHeader, $bTextPrimary, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0135)

$g.DrawString("LAUNCHER ICON PRESETS (4)", $fCardLabel, $bTextMuted, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0085)

# 4 Squircle App Icons Row (Default, Dawn, Focus, Vault)
$iconRowH = [float]($H * 0.145)
$iconRowPath = New-RoundedRectPath ($slot1X + $pad1) $curY1 ($slot1W - 2 * $pad1) $iconRowH 14.0
$g.FillPath($bSurfacePanel, $iconRowPath)
$g.DrawPath($pBorderSubtle, $iconRowPath)

$presets = @("Default", "Dawn", "Focus", "Vault")
$sqSize = [float]($iconRowH * 0.48)
$sqGap  = [float]((($slot1W - 2 * $pad1) - (4 * $sqSize)) / 5.0)
for ($pi = 0; $pi -lt 4; $pi++) {
    $sx = $slot1X + $pad1 + $sqGap + $pi * ($sqSize + $sqGap)
    $sy = $curY1 + 14.0
    $sqPath = New-RoundedRectPath $sx $sy $sqSize $sqSize 14.0
    $g.FillPath($(if ($pi -eq 0) { $bPrimaryAccent } else { $bSurfaceElev }), $sqPath)
    if ($pi -eq 0) {
        $ringPath = New-RoundedRectPath ($sx - 4.0) ($sy - 4.0) ($sqSize + 8.0) ($sqSize + 8.0) 16.0
        $g.DrawPath($pAccentBorder, $ringPath)
        $ringPath.Dispose()
    } else {
        $g.DrawPath($pBorderSubtle, $sqPath)
    }
    $g.DrawString($presets[$pi], $fCardSub, $(if ($pi -eq 0) { $bPrimaryAccent } else { $bTextMuted }), $sx + 4.0, $sy + $sqSize + 8.0)
    $sqPath.Dispose()
}
$iconRowPath.Dispose()
$curY1 += $iconRowH + 12.0

$g.DrawString("3X/DAY SCHEDULED ROTATION (3H COOLDOWN)", $fCardLabel, $bTextMuted, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0085)

$scheduleSlots = @(
    @{ Name = "Morning Slot"; Time = "06:00 AM"; Meta = "Dawn Icon $bullet Active Alias" },
    @{ Name = "Afternoon Slot"; Time = "12:00 PM"; Meta = "Focus Icon $bullet Study Mode" },
    @{ Name = "Night Slot"; Time = "08:00 PM"; Meta = "Vault Icon $bullet Dark Slate" }
)
foreach ($c in $scheduleSlots) {
    $rowH = [float]($H * 0.076)
    $rowPath = New-RoundedRectPath ($slot1X + $pad1) $curY1 ($slot1W - 2 * $pad1) $rowH 12.0
    $g.FillPath($bSurfaceElev, $rowPath)
    $g.DrawPath($pBorderSubtle, $rowPath)
    $g.DrawString($c.Name, $fCardBody, $bTextPrimary, $slot1X + $pad1 + 10.0, $curY1 + 7.0)
    $g.DrawString($c.Time, $fCardLabel, $bPrimaryAccent, $slot1X + $slot1W - $pad1 - ($slot1W * 0.28), $curY1 + 8.0)
    $g.DrawString($c.Meta, $fCardSub, $bTextMuted, $slot1X + $pad1 + 10.0, $curY1 + ($rowH * 0.52))
    $rowPath.Dispose()
    $curY1 += $rowH + 8.0
}

$g.Restore($state1)
$g.DrawPath($pBorderSubtle, $pathSlot1)
$pathSlot1.Dispose()

# ==============================================================================
# 2B. Render Slot 3 (Far-Right Tallest Layer): Folder Settings — Animated GIF Cover & Badge Backing Studio
# ==============================================================================
$pathSlot3 = New-TopRoundedRectPath $slot3X $slot3Y $slot3W $slot3H 22.0
$state3 = $g.Save()
$g.SetClip($pathSlot3)
$g.FillPath($bBgCanvas, $pathSlot3)

$pad3 = [float]($slot3W * 0.065)
$curY3 = $slot3Y + 18.0

$g.DrawString("Folder Settings", $fCardHeader, $bTextPrimary, $slot3X + $pad3, $curY3)
$curY3 += [float]($W * 0.013)

$g.DrawString("LIVE PREVIEW $bullet 6X ANIMATED GIF COVER", $fCardLabel, $bTextMuted, $slot3X + $pad3, $curY3)
$curY3 += [float]($W * 0.0085)

# Simulated Folder Card with Animated GIF Cover & 44dp Custom PNG Badge
$simH = [float]($H * 0.175)
$simPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $simH 16.0
$g.FillPath($bSurfacePanel, $simPath)
$g.DrawPath($pAccentBorder, $simPath)

# 44dp Custom Badge Box in Top-Left + GIF Pill in Top-Right
$bdgPath = New-RoundedRectPath ($slot3X + $pad3 + 12.0) ($curY3 + 12.0) 36.0 36.0 10.0
$g.FillPath($bSurfaceElev, $bdgPath)
$g.DrawPath($pAccentBorder, $bdgPath)
$g.DrawString("PNG", $fCardLabel, $bPrimaryAccent, $slot3X + $pad3 + 16.0, $curY3 + 23.0)
$bdgPath.Dispose()

$gifPill = New-RoundedRectPath ($slot3X + $slot3W - $pad3 - 54.0) ($curY3 + 12.0) 42.0 22.0 11.0
$g.FillPath($bPrimaryAccent, $gifPill)
$g.DrawString("GIF", $fCardLabel, $bTextPrimary, $slot3X + $slot3W - $pad3 - 45.0, $curY3 + 16.0)
$gifPill.Dispose()

$g.DrawString("Physics II Vault", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($simH * 0.50))
$g.DrawString("24 Notes $bullet 360x202 @ 15fps (0.9 MB)", $fCardSub, $bTextMuted, $slot3X + $pad3 + 12.0, $curY3 + ($simH * 0.72))
$simPath.Dispose()
$curY3 += $simH + 12.0

# Badge Backing & Border Studio
$g.DrawString("BADGE BACKING & BORDER STUDIO", $fCardLabel, $bTextMuted, $slot3X + $pad3, $curY3)
$curY3 += [float]($W * 0.0085)

$hsvH = [float]($H * 0.165)
$hsvRect = [System.Drawing.RectangleF]::new($slot3X + $pad3, $curY3, $slot3W - 2 * $pad3, $hsvH)
$hsvPath = New-RoundedRectPath $hsvRect.X $hsvRect.Y $hsvRect.Width $hsvRect.Height 14.0
$g.FillPath($bSurfacePanel, $hsvPath)
$g.DrawPath($pBorderSubtle, $hsvPath)
$g.DrawString("Show Backing Box", $fCardBody, $bTextPrimary, $hsvRect.X + 12.0, $hsvRect.Y + 10.0)
$g.FillEllipse($bPrimaryAccent, $hsvRect.X + $hsvRect.Width - 28.0, $hsvRect.Y + 10.0, 16.0, 16.0)
$g.DrawString("Border:  [ None ]  [ Accent ]  [ Frosted ]", $fCardSub, $bPrimaryAccent, $hsvRect.X + 12.0, $hsvRect.Y + ($hsvH * 0.44))
$g.DrawString("Icon Fit:  [ Contained (8dp) ]  [ Full-Bleed ]", $fCardSub, $bEmerald, $hsvRect.X + 12.0, $hsvRect.Y + ($hsvH * 0.72))
$hsvPath.Dispose()
$curY3 += $hsvH + 10.0

# Edge-to-Edge Schedule Binding & Note Badge Rows
$hexRowH = [float]($H * 0.068)
$hexPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $hexRowH 12.0
$g.FillPath($bSurfaceElev, $hexPath)
$g.DrawPath($pBorderSubtle, $hexPath)
$g.DrawString("Bind Folder: Physics II", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($hexRowH * 0.26))
$hexPath.Dispose()
$curY3 += $hexRowH + 8.0

$syncPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $hexRowH 12.0
$g.FillPath($bSurfaceElev, $syncPath)
$g.DrawPath($pBorderSubtle, $syncPath)
$g.DrawString("Select 1 Note: Gauss Law Summary", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($hexRowH * 0.26))
$g.FillEllipse($bPrimaryAccent, $slot3X + $slot3W - $pad3 - 26.0, $curY3 + ($hexRowH * 0.25), 16.0, 16.0)
$syncPath.Dispose()
$curY3 += $hexRowH + 8.0

$lockPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $hexRowH 12.0
$g.FillPath($bSurfaceElev, $lockPath)
$g.DrawPath($pBorderSubtle, $lockPath)
$g.DrawString("Pin to Top of Workspace", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($hexRowH * 0.26))
$lockPath.Dispose()

$g.Restore($state3)
$g.DrawPath($pBorderSubtle, $pathSlot3)
$pathSlot3.Dispose()

# ==============================================================================
# 2C. Render Slot 2 (Center-Right Foreground Layer): New Subject Folder Sheet & Left [NOTE] Badge Color Sync
# ==============================================================================
$pathSlot2 = New-TopRoundedRectPath $slot2X $slot2Y $slot2W $slot2H 20.0
$state2 = $g.Save()
$g.SetClip($pathSlot2)
$g.FillPath($bSurfacePanel, $pathSlot2)

$pad2 = [float]($slot2W * 0.075)
$curY2 = $slot2Y + 16.0

$g.DrawString("New Subject Folder", $fCardBody, $bTextPrimary, $slot2X + $pad2, $curY2)
$curY2 += [float]($W * 0.0085)
$g.DrawString("44dp Live Badge $bullet 7 Swatches + [+]", $fCardSub, $bTextMuted, $slot2X + $pad2, $curY2)
$curY2 += [float]($W * 0.0105)

# Note Card with Tinted Top-Left [ NOTE ] Badge & Study Guide Label Pill (No Top-Right Dot)
$cropSize = [float]($slot2W - 2 * $pad2)
$cropH = [float]($cropSize * 0.72)
$cropPath = New-RoundedRectPath ($slot2X + $pad2) $curY2 $cropSize $cropH 14.0
$g.FillPath($bBgCanvas, $cropPath)
$g.DrawPath($pAccentBorder, $cropPath)

$noteBadge = New-RoundedRectPath ($slot2X + $pad2 + 10.0) ($curY2 + 10.0) 68.0 24.0 12.0
$g.FillPath($bAccentSoft, $noteBadge)
$g.DrawString("NOTE", $fCardLabel, $bPrimaryAccent, $slot2X + $pad2 + 22.0, $curY2 + 15.0)
$noteBadge.Dispose()

$lblBadge = New-RoundedRectPath ($slot2X + $pad2 + 86.0) ($curY2 + 10.0) 96.0 24.0 12.0
$g.FillPath($bAccentSoft, $lblBadge)
$g.DrawString("Study Guide", $fCardLabel, $bPrimaryAccent, $slot2X + $pad2 + 96.0, $curY2 + 15.0)
$lblBadge.Dispose()

$g.DrawString("Thermodynamics Notes", $fCardBody, $bTextPrimary, $slot2X + $pad2 + 12.0, $curY2 + ($cropH * 0.44))
$g.DrawString("Chapter 6 Laws & Entropy Problems", $fCardSub, $bTextMuted, $slot2X + $pad2 + 12.0, $curY2 + ($cropH * 0.68))
$cropPath.Dispose()
$curY2 += $cropH + 10.0

# Curated Swatch Strip Indicator
$g.DrawString("Accent Swatches (7 + Custom HSV)", $fCardLabel, $bPrimaryAccent, $slot2X + $pad2, $curY2)
$curY2 += [float]($W * 0.008)
$g.FillRectangle($bTrackInactive, $slot2X + $pad2, $curY2, $cropSize, 5.0)
$g.FillRectangle($bPrimaryAccent, $slot2X + $pad2, $curY2, [float]($cropSize * 0.65), 5.0)
$curY2 += 12.0

$chipW = [float](($cropSize - 8.0) / 3.0)
$chipH = [float]($H * 0.048)
$chips = @("Violet", "Teal", "Emerald")
for ($i = 0; $i -lt 3; $i++) {
    $cx = $slot2X + $pad2 + $i * ($chipW + 4.0)
    $cp = New-RoundedRectPath $cx $curY2 $chipW $chipH ($chipH / 2.0)
    $g.FillPath($bSurfaceElev, $cp)
    $g.DrawPath($pBorderSubtle, $cp)
    $g.DrawString($chips[$i], $fCardLabel, $bTextPrimary, $cx + 9.0, $curY2 + ($chipH * 0.24))
    $cp.Dispose()
}
$curY2 += $chipH + 10.0

# Button-in-Button Widget Sync + Preview pill
$wsH = [float]($H * 0.062)
$wsPath = New-RoundedRectPath ($slot2X + $pad2) $curY2 $cropSize $wsH 12.0
$g.FillPath($bSurfaceElev, $wsPath)
$g.DrawPath($pBorderSubtle, $wsPath)
$g.DrawString("Widget Sync", $fCardLabel, $bTextPrimary, $slot2X + $pad2 + 8.0, $curY2 + ($wsH * 0.28))
$prevH = [float]($wsH * 0.64)
$prevPill = New-RoundedRectPath ($slot2X + $pad2 + $cropSize * 0.58) ($curY2 + $wsH * 0.18) ($cropSize * 0.38) $prevH ($prevH / 2.0)
$g.FillPath($bPrimaryAccent, $prevPill)
$g.DrawString("Preview", $fCardLabel, $bTextPrimary, $slot2X + $pad2 + $cropSize * 0.63, $curY2 + ($wsH * 0.28))
$prevPill.Dispose()
$wsPath.Dispose()

$g.Restore($state2)
$g.DrawPath($pAccentBorder, $pathSlot2)
$pathSlot2.Dispose()

# ==============================================================================
# 3. Left-Column ElmsSans Typography (Aligned to Pre-Baked Top-Left Logo X=188 on 2460x1000)
# ==============================================================================
$textLeftX = [float][Math]::Round($W * (188.0 / 2460.0))
$titleTopY = [float][Math]::Round($H * (352.0 / 1000.0))

$fontTitleSize = [float][Math]::Round($W * 0.0525)
$fontDescSize  = [float][Math]::Round($W * 0.0172)
$fTitle = [System.Drawing.Font]::new($elmsFamily, $fontTitleSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fDesc  = [System.Drawing.Font]::new($elmsFamily, $fontDescSize, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)

$sfTypo = [System.Drawing.StringFormat]::GenericTypographic
$sfTypo.FormatFlags = $sfTypo.FormatFlags -bor [System.Drawing.StringFormatFlags]::MeasureTrailingSpaces

$verSize = $g.MeasureString($Version, $fTitle, [System.Drawing.PointF]::new(0, 0), $sfTypo)

$bPureWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$g.DrawString($Version, $fTitle, $bPureWhite, $textLeftX, $titleTopY, $sfTypo)

# Signature Blue Gradient (#1E88E5 to #42A5F5) for Codename ("Hyperion")
$codeX = $textLeftX + $verSize.Width + [float]($W * 0.016)
$codeSize = $g.MeasureString($Codename, $fTitle, [System.Drawing.PointF]::new(0, 0), $sfTypo)
$codeRect = [System.Drawing.RectangleF]::new($codeX, $titleTopY, [Math]::Max($codeSize.Width, 40.0), [Math]::Max($codeSize.Height, 40.0))
$bCodeGrad = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    $codeRect,
    [System.Drawing.Color]::FromArgb(255, 30, 136, 229),  # #1E88E5
    [System.Drawing.Color]::FromArgb(255, 66, 165, 245),  # #42A5F5
    [System.Drawing.Drawing2D.LinearGradientMode]::Horizontal
)
$g.DrawString($Codename, $fTitle, $bCodeGrad, $codeX, $titleTopY, $sfTypo)

# Concise English Subtitle Description directly below 2.6.0
$descTopY = $titleTopY + $verSize.Height + [float]($H * 0.032)
$descMaxW = [float]($slot1X - $textLeftX - ($W * 0.035))
$descRect = [System.Drawing.RectangleF]::new($textLeftX, $descTopY, $descMaxW, [float]($H * 0.28))
$g.DrawString($Description, $fDesc, $bPureWhite, $descRect, $sfTypo)

# ==============================================================================
# 4. Verify Pre-Baked Top-Left Logos Were Never Modified
# ==============================================================================
for ($ly = 0; $ly -lt $logoGuardRect.Height; $ly += 4) {
    for ($lx = 0; $lx -lt $logoGuardRect.Width; $lx += 4) {
        $origPx = $logoGuardSnapshot.GetPixel($lx, $ly)
        $currPx = $canvas.GetPixel($logoGuardRect.X + $lx, $logoGuardRect.Y + $ly)
        if ($origPx.ToArgb() -ne $currPx.ToArgb()) {
            throw "Logo protection rule violated at ($($logoGuardRect.X + $lx), $($logoGuardRect.Y + $ly))"
        }
    }
}

$outDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$canvas.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$bCodeGrad.Dispose()
$bPureWhite.Dispose()
$fTitle.Dispose()
$fDesc.Dispose()
$fCardHeader.Dispose()
$fCardSub.Dispose()
$fCardLabel.Dispose()
$fCardBody.Dispose()
$logoGuardSnapshot.Dispose()
$g.Dispose()
$canvas.Dispose()
$baseImg.Dispose()
$pfc.Dispose()

Write-Output "Generated v2.6+ Redesigned Banner: $OutputPath"
