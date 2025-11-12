package com.example.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PythonVersionUtilsTest {

    @Test
    void testPyprojectToml() {
        String content = """
            [project]
            requires-python = ">=3.10,<3.12"
            """;
        assertEquals("langage: python:3.10", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testSetupPy() {
        String content = """
            from setuptools import setup
            setup(name="app", python_requires=">=3.9,<3.12")
            """;
        assertEquals("langage: python:3.9", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testSetupCfg() {
        String content = """
            [options]
            python_requires = >=3.8
            """;
        assertEquals("langage: python:3.8", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testPipfile() {
        String content = """
            [requires]
            python_version = "3.11"
            """;
        assertEquals("langage: python:3.11", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testEnvironmentYml() {
        String content = """
            name: env
            dependencies:
              - python=3.9
            """;
        assertEquals("langage: python:3.9", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testDockerfile() {
        String content = "FROM python:3.12-slim";
        assertEquals("langage: python:3.12", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testPythonVersionFile() {
        String content = "3.11.5";
        assertEquals("langage: python:3.11.5", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testRuntimeTxt() {
        String content = "python-3.10.13";
        assertEquals("langage: python:3.10.13", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testCommentRequirements() {
        String content = """
            # Python 3.9 required
            flask==2.0.3
            """;
        assertEquals("langage: python:3.9", PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testNoVersionFound() {
        String content = "dependencies:\n  - pandas\n  - numpy";
        assertNull(PythonVersionUtils.detectPythonVersion(content));
    }

    @Test
    void testEmptyOrNull() {
        assertNull(PythonVersionUtils.detectPythonVersion(""));
        assertNull(PythonVersionUtils.detectPythonVersion("   "));
        assertNull(PythonVersionUtils.detectPythonVersion(null));
    }
}