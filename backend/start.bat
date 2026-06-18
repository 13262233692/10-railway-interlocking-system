@echo off
chcp 65001 >nul
echo ========================================
echo   铁路联锁系统启动脚本
echo   Railway Interlocking System
echo ========================================
echo.

echo [1/3] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Java环境，请先安装JDK 11或更高版本
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java环境检测通过
java -version
echo.

echo [2/3] 编译项目...
call mvnw.cmd clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] 项目编译失败，请检查错误信息
    pause
    exit /b 1
)
echo [OK] 项目编译成功
echo.

echo [3/3] 启动应用...
echo.
echo ========================================
echo   服务启动中，请稍候...
echo   访问地址: http://localhost:8080/api
echo   WebSocket: ws://localhost:8080/api/ws/interlocking
echo   按 Ctrl+C 停止服务
echo ========================================
echo.

java -jar target/interlocking-system-1.0.0.jar
pause
