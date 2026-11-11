"""Safety checks for the interactive bootstrap; never contacts Azure."""
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "bootstrap", Path(__file__).resolve().parents[1] / "scripts/bootstrap_staging_secrets.py")
bootstrap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bootstrap)


class SecretBootstrapTests(unittest.TestCase):
    def run_main(self, names, answers, existing=False):
        account = '{"tenantId":"' + bootstrap.TENANT + '","user":{"type":"user"}}'
        def fake_request(token, name, method="GET", value=None):
            if method == "GET":
                return {"value": [{"id": "old-version"}] if existing else []}
            return {"id": "https://vault/secrets/" + name + "/version-1", "value": value}
        with patch("sys.argv", ["bootstrap", "--names", *names]), \
             patch.object(bootstrap.sys.stdin, "isatty", return_value=True), \
             patch.object(bootstrap, "cli", side_effect=[account, "hidden-test-token"]), \
             patch.object(bootstrap, "request", side_effect=fake_request) as request, \
             patch.object(bootstrap.getpass, "getpass", side_effect=answers), \
             contextlib.redirect_stdout(io.StringIO()) as output:
            bootstrap.main()
            return request, output.getvalue()

    def test_redirected_input_refused_before_cli(self):
        with patch("sys.argv", ["bootstrap"]), \
             patch.object(bootstrap.sys.stdin, "isatty", return_value=False), \
             patch.object(bootstrap, "cli") as cli:
            with self.assertRaisesRegex(RuntimeError, "interactively"):
                bootstrap.main()
            cli.assert_not_called()

    def test_existing_secret_preserved(self):
        with self.assertRaisesRegex(RuntimeError, "already exists"):
            self.run_main(["db-password"], [], existing=True)

    def test_invalid_key_refused_before_any_write(self):
        with self.assertRaisesRegex(RuntimeError, "exactly 32"):
            self.run_main(["pii-encryption-key"], ["short", "short"])

    def test_output_contains_only_name_and_version(self):
        request, output = self.run_main(["db-password"], ["test-only-password", "test-only-password"])
        self.assertIn("Stored db-password; version version-1", output)
        self.assertNotIn("test-only-password", output)
        self.assertNotIn("hidden-test-token", output)
        self.assertEqual(request.call_args_list[-1].args[2], "PUT")


if __name__ == "__main__":
    unittest.main()
