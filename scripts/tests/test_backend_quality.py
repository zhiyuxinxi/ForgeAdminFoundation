"""Contract checks use the real pinned scanners when their runtime is prepared."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("backend_quality", Path(__file__).parents[1] / "backend-quality.py")
quality = importlib.util.module_from_spec(spec)
spec.loader.exec_module(quality)


class SourceQualityContractTest(unittest.TestCase):
    def test_checkstyle_failed_exit_is_not_masked_by_existing_findings(self):
        issues = [{"severity": "error"}, {"severity": "warning"}]
        self.assertFalse(quality.checkstyle_exit_error(1, issues, []))
        for code in (-2, 254, 4294967294, 0, 2):
            self.assertTrue(quality.checkstyle_exit_error(code, issues, []))

    def scan(self, root, source):
        (root / "Example.java").write_text(source, encoding="utf-8")
        capture = io.StringIO()
        # Isolated files are never added to application sources or the Git index.
        with patch.object(quality, "ROOT", root), patch("sys.argv", ["backend-quality", "fast", "--file", "Example.java"]):
            with contextlib.redirect_stdout(capture):
                code = quality.main()
        result = json.loads(capture.getvalue())
        report = json.loads(Path(result["report"]).read_text(encoding="utf-8"))
        return code, report

    def test_real_scanners_accept_valid_source_and_reject_changed_error_patterns(self):
        self.assertTrue((quality.TOOLS / "target/runtime/classpath.txt").is_file(), "Prepare scanner runtime first")
        with tempfile.TemporaryDirectory(prefix="forge-quality-contract-") as directory:
            root = Path(directory).resolve()
            code, report = self.scan(root, "class Example {\n    int increment(int value) {\n        return value + 1;\n    }\n}\n")
            self.assertEqual((code, report["status"], report["errors"]), (0, "passed", []), report)
            code, report = self.scan(root, "class Example {\n    void broken() {\n        try { int a = 1; int b = 2; } catch (Throwable error) { }\n    }\n}\n")
            self.assertEqual((code, report["status"]), (1, "findings"), report)
            self.assertEqual(report["errors"], [])
            self.assertEqual({item["tool"] for item in report["findings"]}, {"pmd", "checkstyle"})
            self.assertTrue(any(item["rule"] == "EmptyCatchBlock" for item in report["findings"]))

    def test_parse_failure_is_execution_error_not_clean_scan(self):
        with tempfile.TemporaryDirectory(prefix="forge-quality-contract-") as directory:
            code, report = self.scan(Path(directory).resolve(), "class Example { broken java source ")
            self.assertEqual((code, report["status"]), (2, "error"), report)
            self.assertTrue(report["errors"])
            self.assertEqual(len(report["tools"]), 2, report)
            self.assertEqual(report["tools"][0]["exitCode"], 5, report)

    def test_real_scanners_enforce_method_count_and_coupling_boundaries(self):
        def fixture(methods, types):
            fields = "".join(f"    Dependency{i} dependency{i};\n" for i in range(types))
            bodies = "".join(f"    int operation{i}() {{\n        return {i};\n    }}\n" for i in range(methods))
            dependencies = "".join(f"class Dependency{i} {{}}\n" for i in range(types))
            return "class Example {\n" + fields + bodies + "}\n" + dependencies

        with tempfile.TemporaryDirectory(prefix="forge-quality-structure-") as directory:
            root = Path(directory).resolve()
            code, report = self.scan(root, fixture(50, 20))
            self.assertEqual((code, report["status"], report["errors"]), (0, "passed", []), report)
            self.assertEqual(report["findingCounts"], {"structure": 0, "code": 0})
            code, report = self.scan(root, fixture(51, 21))
            self.assertEqual((code, report["status"], report["errors"]), (1, "findings", []), report)
            self.assertEqual({item["rule"] for item in report["findings"]},
                             {"TooManyMethods", "CouplingBetweenObjects"}, report)
            self.assertEqual(report["findingCounts"], {"structure": 2, "code": 0})
            for item in report["findings"]:
                self.assertGreaterEqual(item["endLine"], item["line"])
                self.assertTrue(item["documentationUrl"].startswith("https://docs.pmd-code.org/"))

    def test_scope_rejects_outside_repository(self):
        with tempfile.TemporaryDirectory(prefix="forge-quality-scope-") as directory:
            root = Path(directory).resolve()
            with patch.object(quality, "ROOT", root):
                args = type("Args", (), {"file": ["../outside.java"], "base": None})()
                with self.assertRaises(ValueError):
                    quality.selected_files(args)


if __name__ == "__main__":
    unittest.main()
