@echo off
set PY=C:\Program Files\Unity\Hub\Editor\6000.3.6f1\Editor\Data\PlaybackEngines\WebGLSupport\BuildTools\Emscripten\python\python.exe
if exist "%PY%" (
  "%PY%" -m http.server 8000
) else (
  python -m http.server 8000
)
