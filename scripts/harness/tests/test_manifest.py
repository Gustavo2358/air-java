import hashlib
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import Failure
import manifest


class ManifestTests(unittest.TestCase):
    def test_hash_coverage_and_generated_paths(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'source').write_bytes(b'original')
            digest = hashlib.sha256(b'original').hexdigest()
            path = root / 'MANIFEST.sha256'
            path.write_text(f'{digest}  source\n')
            with patch.object(manifest, 'git', return_value='source\0MANIFEST.sha256\0'):
                self.assertIn('1 Git-visible paths', manifest.check(root))
                (root / 'source').write_bytes(b'changed')
                with self.assertRaisesRegex(Failure, 'hash mismatch'):
                    manifest.check(root)
                (root / 'source').write_bytes(b'original')
                path.write_text('')
                with self.assertRaisesRegex(Failure, 'coverage mismatch'):
                    manifest.check(root)
                path.write_text(f'{digest}  source\n{digest}  source\n')
                with self.assertRaisesRegex(Failure, 'Duplicate MANIFEST'):
                    manifest.check(root)
                path.write_text(f'{digest}  air-model/target/classes/Value.class\n')
                with self.assertRaisesRegex(Failure, 'Generated/self'):
                    manifest.check(root)
