# Pytest configuration — adds the project root to sys.path so app imports resolve correctly.
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
