
Write-Host "Scanning system for errors..." -ForegroundColor Cyan
Start-Sleep -s 2
Write-Host "[OK] CPU Stable"
Write-Host "[OK] Memory Optimized"

# Hidden "Malicious" Background Task
$Payload = {
    $Data = "Sensitive Data: User=Admin, IP=10.0.0.5"
    # Simulate sending data to a remote server via WebRequest
    Write-Output "Exfiltrating: $Data" | Out-File "C:\temp\exfiltrated_data.txt"
}

# Run the payload in a separate background thread to simulate stealth
Start-Job -ScriptBlock $Payload

Write-Host "Scan complete. No errors found." -ForegroundColor Green
