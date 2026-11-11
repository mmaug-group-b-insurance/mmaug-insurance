"""Interactive bootstrap from a trusted VNet-connected terminal; never run in CI.

Requires Python 3, Azure CLI signed in as an authorized human user, private DNS,
and temporary Key Vault Secrets Officer access. No third-party Python packages.
Existing secret names are refused to avoid accidental rotation. No keys generated.
"""
import argparse
import getpass
import json
import subprocess
import sys
import urllib.error
import urllib.request

VAULT = "https://kv-mmaug-claims-stg-01.vault.azure.net"
TENANT = "71beefd7-29f4-4b9e-9cb2-e78b5dc851ad"
NAMES = ("db-password", "docintel-key", "ai-key", "pii-encryption-key")


def cli(*args):
    result = subprocess.run(["az", *args, "--only-show-errors"],
                            capture_output=True, text=True, check=False)
    if result.returncode:
        raise RuntimeError("Azure CLI command failed; check your sign-in and permissions separately.")
    return result.stdout.strip()


def request(token, name, method="GET", value=None):
    suffix = "/versions" if method == "GET" else ""
    url = f"{VAULT}/secrets/{name}{suffix}?api-version=7.4"
    body = None if value is None else json.dumps({"value": value}).encode("utf-8")
    req = urllib.request.Request(url, data=body, method=method, headers={
        "Authorization": "Bearer " + token, "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if method == "GET" and error.code == 404:
            return {"value": []}
        # Never print response bodies, which may contain request/provider details.
        raise RuntimeError(f"Vault {method} failed with HTTP {error.code} for {name}.") from None
    except urllib.error.URLError:
        raise RuntimeError("Vault connection failed; check private DNS, routing and TLS trust.") from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--names", nargs="+", choices=NAMES, default=list(NAMES))
    args = parser.parse_args()
    if not sys.stdin.isatty():
        raise RuntimeError("Run interactively in a trusted terminal; redirected input is refused.")
    account = json.loads(cli("account", "show", "-o", "json"))
    if account.get("tenantId") != TENANT or account.get("user", {}).get("type") != "user":
        raise RuntimeError("Sign in as the authorized bootstrap user in the project tenant.")
    token = cli("account", "get-access-token", "--resource", "https://vault.azure.net",
                "--query", "accessToken", "-o", "tsv")
    names = list(dict.fromkeys(args.names))
    for name in names:
        if request(token, name).get("value"):
            raise RuntimeError(f"Secret {name} already exists. Preserve its version; review separately.")
    values = {}
    for name in names:
        value = getpass.getpass(f"{name} (hidden): ")
        if not value or value != getpass.getpass(f"Confirm {name} (hidden): "):
            raise RuntimeError("Secret is empty or confirmation differs; nothing has been written.")
        if name == "pii-encryption-key" and len(value.encode("utf-8")) != 32:
            raise RuntimeError("PII key must be exactly 32 UTF-8 bytes; nothing has been written.")
        values[name] = value
    try:
        for name in names:
            response = request(token, name, "PUT", values[name])
            version = response["id"].rsplit("/", 1)[-1]
            # PUT response contains the value; only emit the non-secret name/version.
            print(f"Stored {name}; version {version}")
            response.clear()
    finally:
        values.clear()
        token = ""
    # Clearing references is best effort, not a guarantee of zeroized Python memory.


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, FileNotFoundError) as error:
        print(str(error), file=sys.stderr)
        print("If any write succeeded, preserve it and rerun only missing names using --names.", file=sys.stderr)
        sys.exit(1)
