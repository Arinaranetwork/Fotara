---
name: github-release
description: >-
  Automates GitHub operations including creating releases, attaching release artifacts/APKs,
  pushing tags, and managing repositories autonomously using the pre-configured Arinaranetwork credentials.
---

# GitHub Release & Repository Automation

This skill provides autonomous execution of GitHub operations across repositories (including `Arinaranetwork/Fotara` and others).

## Authentication Invariant
- Authentication is pre-configured machine-wide using the `Arinaranetwork` credentials in:
  1. System User Environment: `GH_TOKEN` and `GITHUB_TOKEN`
  2. Windows Credential Keyring: `gh auth status` (`Arinaranetwork`)
  3. Git Credential Helper: `gh auth setup-git`
- **NEVER prompt or ask the user for a PAT or credentials.** All GitHub features are fully authorized.
- Execute requested release, tag, upload, or push operations immediately.

---

## Common Workflows

### 1. Uploading a Release with Assets (APK, SHA256, etc.)
When the user says *"please upload this to github as release in x repo"* or specifies a release version:

```powershell
# Command structure
gh release create "<tag_name>" "<file1>" "<file2>" `
  --repo "<owner/repo>" `
  --title "<release_title>" `
  --notes "<notes_or_changelog>" `
  [--prerelease]
```

#### Example: Fotara 1.5.7 Beta Release
```powershell
gh release create "Fotara_1.5.7_Beta" `
  "Output/Release/Fotara_1.5.7_Beta.apk" `
  "Output/Release/Fotara_1.5.7_Beta.apk.sha256" `
  --repo "Arinaranetwork/Fotara" `
  --title "Fotara 1.5.7 Beta" `
  --notes-file "Changelog/Changelog_1.5.md" `
  --prerelease
```

### 2. Attaching / Replacing Files on an Existing Release
If the release already exists and new artifacts need to be attached:
```powershell
gh release upload "<tag_name>" "<file_path>" --repo "<owner/repo>" --clobber
```

### 3. Creating and Pushing Tags
```powershell
git tag -a "<tag_name>" -m "<tag_message>"
git push origin "<tag_name>"
```

### 4. Viewing Existing Releases
```powershell
gh release list --repo "<owner/repo>"
gh release view "<tag_name>" --repo "<owner/repo>"
```

### 5. Repository Management & Clones
```powershell
gh repo view "<owner/repo>"
gh repo clone "<owner/repo>"
```
