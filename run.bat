@echo off
REM Bangla Compiler - One Click Run
REM ================================

echo.
echo ========== BANGLA COMPILER ==========
echo.

REM Check if bin folder exists
if not exist "bin" (
    echo ERROR: bin folder not found!
    echo Please compile the project first.
    pause
    exit /b 1
)

REM Check if input.bn exists
if not exist "input.bn" (
    echo ERROR: input.bn file not found!
    pause
    exit /b 1
)

REM Run the compiler
echo Running Bangla Compiler...
echo.
java -cp bin Main input.bn output.txt

REM Check if execution was successful
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========== SUCCESS ==========
    echo.
    echo Output files generated:
    echo - output.txt (Compilation details)
    echo - output.py (Generated Python code)
    echo.
    echo Opening output.txt...
    timeout /t 2
    start notepad output.txt
) else (
    echo.
    echo ========== ERROR ==========
    echo Compilation failed!
    pause
    exit /b 1
)

pause
