# Signing OpenFileBot Releases

Optional if you want a signed release.

## 1) Create a dedicated release signing key (recommended)

Use a dedicated key (or better: dedicated signing subkey) for release artifacts.
Do not reuse one key for all unrelated systems.

### Install tools (Ubuntu / Debian)

```bash
sudo apt-get update
sudo apt-get install -y gnupg
```

### Generate a signing key quickly

```bash
gpg --full-generate-key
```

Recommended choices:

- Type: RSA and RSA
- Size: 4096
- Expiry: 1y (rotate regularly)
- UID: something like `OpenFileBot Release <releases@example.org>`

List keys:

```bash
gpg --list-secret-keys --keyid-format=long
```

Export public key (for users):

```bash
gpg --armor --export <KEY_ID> > openfilebot-release-public.asc
```

Export private key for CI (store securely, never commit):

```bash
gpg --armor --export-secret-keys <KEY_ID> > openfilebot-release-private.asc
base64 -w0 openfilebot-release-private.asc > openfilebot-release-private.asc.b64
```

## 2) Local signing (no prompts during build)

### Option A: build unsigned, then sign artifacts

```bash
export RELEASE_GPG_KEY_ID=<KEY_ID>
export RELEASE_GPG_PASSPHRASE='<PASSPHRASE>'

ant -lib /usr/share/java/ivy.jar clean resolve fatjar deb

for f in dist/*.jar dist/*.deb dist/*.changes; do
  [ -f "$f" ] || continue
  gpg --batch --yes --pinentry-mode loopback --passphrase "$RELEASE_GPG_PASSPHRASE" \
    --local-user "$RELEASE_GPG_KEY_ID" --armor --detach-sign "$f"
done
```

Verify one file:

```bash
gpg --verify dist/<artifact>.asc dist/<artifact>
```

## 3) Security notes

- Rotate signing subkeys periodically.
- Keep a revocation certificate offline.
- Restrict who can access CI secrets and release workflows.
