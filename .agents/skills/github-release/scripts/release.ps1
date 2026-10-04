<#
.SYNOPSIS
  Autonomous GitHub Release and Asset Upload Tool for Arinara Network.
.DESCRIPTION
  Creates or updates a GitHub release using the pre-configured Arinaranetwork token,
  attaching release artifacts (APKs, sha256 hashes, etc.) without requiring manual PAT entry.
.PARAMETER Repo
  GitHub repository in owner/repo format. Defaults to "Arinaranetwork/Fotara".
.PARAMETER Tag
  Release tag name (e.g. "Fotara_1.5.7_Beta").
.PARAMETER Title
  Release title (e.g. "Fotara 1.5.7 Beta").
.PARAMETER NotesFile
  Path to release notes or changelog markdown file.
.PARAMETER Notes
  Direct string notes for the release.
.PARAMETER Artifacts
  One or more file paths to upload as release assets.
.PARAMETER Prerelease
  Marks the release as a pre-release (defaults to true if Tag contains "Beta").
#>
param(
    [string]$Repo = "Arinaranetwork/Fotara",
    [Parameter(Mandatory=$true)]
    [string]$Tag,
    [string]$Title = "",
    [string]$NotesFile = "",
    [string]$Notes = "",
    [string[]]$Artifacts = @(),
    [switch]$Prerelease
)

$ErrorActionPreference = "Stop"

# Ensure environment token is available
if (-not $env:GH_TOKEN -and -not $env:GITHUB_TOKEN) {
    $userToken = [System.Environment]::GetEnvironmentVariable("GH_TOKEN", [System.EnvironmentVariableTarget]::User)
    if ($userToken) {
        $env:GH_TOKEN = $userToken
        $env:GITHUB_TOKEN = $userToken
    }
}

if (-not $Title) {
    $Title = $Tag.Replace("_", " ")
}

if (-not $PSBoundParameters.ContainsKey('Prerelease')) {
    if ($Tag -match "Beta") {
        $Prerelease = $true
    }
}

Write-Host "==> Checking release for tag '$Tag' in repo '$Repo'..." -ForegroundColor Cyan

# Check if release exists
$existingRelease = $null
try {
    $existingRelease = gh release view "$Tag" --repo "$Repo" --json tagName,url 2>$null | ConvertFrom-Json
} catch {
    $existingRelease = $null
}

if ($existingRelease) {
    Write-Host "==> Release '$Tag' already exists ($($existingRelease.url)). Uploading artifacts..." -ForegroundColor Yellow
    foreach ($artifact in $Artifacts) {
        if (Test-Path $artifact) {
            Write-Host "Uploading '$artifact'..." -ForegroundColor Green
            gh release upload "$Tag" "$artifact" --repo "$Repo" --clobber
        } else {
            Write-Warning "Artifact not found: $artifact"
        }
    }
    Write-Host "==> Artifact upload completed successfully!" -ForegroundColor Green
    return
}

# Create new release
Write-Host "==> Creating new release '$Tag'..." -ForegroundColor Cyan
$cmdArgs = @("release", "create", "$Tag", "--repo", "$Repo", "--title", "$Title")

if ($Prerelease) {
    $cmdArgs += "--prerelease"
}

if ($NotesFile -and (Test-Path $NotesFile)) {
    $cmdArgs += @("--notes-file", "$NotesFile")
} elseif ($Notes) {
    $cmdArgs += @("--notes", "$Notes")
} else {
    $cmdArgs += @("--generate-notes")
}

foreach ($artifact in $Artifacts) {
    if (Test-Path $artifact) {
        $cmdArgs += "$artifact"
    } else {
        Write-Warning "Artifact not found: $artifact"
    }
}

& gh @cmdArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host "==> Release '$Tag' created and published successfully!" -ForegroundColor Green
} else {
    Write-Error "Failed to create release '$Tag'. Exit code: $LASTEXITCODE"
}
