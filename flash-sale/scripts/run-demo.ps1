param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$ProductId = 'PROD-001',
    [int]$TimeoutSeconds = 20
)

$ErrorActionPreference = 'Stop'

function Set-Stock([int]$Quantity) {
    $body = @{ quantity = $Quantity } | ConvertTo-Json
    Invoke-RestMethod -Method Put -Uri "$BaseUrl/v1/inventory/$ProductId" `
        -ContentType 'application/json' -Body $body | Out-Null
}

function New-Order([bool]$SimulateFailure, [string]$Reason) {
    $correlationId = [guid]::NewGuid().ToString()
    $body = @{
        produto_id = $ProductId
        quantidade = 2
        cliente_cpf = '50329291076'
        cliente_nome = 'Cliente de demonstracao'
        simular_falha = $SimulateFailure
        motivo_falha = $Reason
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/v1/orders" `
        -Headers @{ 'X-Correlation-Id' = $correlationId } `
        -ContentType 'application/json' -Body $body

    [pscustomobject]@{
        OrderId = $response.pedido_id
        CorrelationId = $response.correlation_id
    }
}

function Wait-ForStatus([string]$OrderId, [string]$ExpectedStatus, [string]$CorrelationId) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $order = Invoke-RestMethod -Method Get -Uri "$BaseUrl/v1/orders/$OrderId" `
                -Headers @{ 'X-Correlation-Id' = $CorrelationId }
            if ($order.status -eq $ExpectedStatus) { return $order }
        } catch {
            $statusCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
            if ($statusCode -ne 404) { throw }
        }
        Start-Sleep -Milliseconds 300
    } while ((Get-Date) -lt $deadline)

    throw "Pedido $OrderId nao chegou ao status $ExpectedStatus em $TimeoutSeconds segundos. Consulte os logs dos dois servicos."
}

Write-Host 'Cenario 1: pedido confirmado' -ForegroundColor Cyan
Set-Stock 10
$successOrder = New-Order $false ''
$confirmed = Wait-ForStatus $successOrder.OrderId 'CONFIRMED' $successOrder.CorrelationId
Write-Host "CONFIRMED | pedido=$($confirmed.pedido_id) | correlationId=$($confirmed.correlation_id)"

Write-Host 'Cenario 2: falha e compensacao' -ForegroundColor Cyan
Set-Stock 10
$failedOrder = New-Order $true 'Falha simulada para demonstracao'
$cancelled = Wait-ForStatus $failedOrder.OrderId 'CANCELLED' $failedOrder.CorrelationId
$stock = Invoke-RestMethod -Method Get -Uri "$BaseUrl/v1/inventory/$ProductId"
Write-Host "CANCELLED | pedido=$($cancelled.pedido_id) | motivo=$($cancelled.motivo_falha)"
Write-Host "Estoque apos compensacao: $($stock.available) | correlationId=$($cancelled.correlation_id)"

if ($stock.available -ne 10) {
    throw "Esperado estoque 10 depois da compensacao, recebido $($stock.available)."
}

Write-Host 'Demonstracao concluida.' -ForegroundColor Green