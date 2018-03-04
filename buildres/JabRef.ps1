function Respond($response) {
    $jsonResponse = $response | ConvertTo-Json

    try {
        $writer = New-Object System.IO.BinaryWriter([System.Console]::OpenStandardOutput())
        $writer.Write([int]$jsonResponse.Length)
        $writer.Write([System.Text.Encoding]::UTF8.GetBytes($jsonResponse))
        $writer.Close()
    } finally {
        $writer.Dispose()
    }
}

$jabRefJarFileName = "JabRef-4.2-dev-fat.jar"
$jabRefJar = [System.IO.Path]::Combine($PSScriptRoot, $jabRefJarFileName)

try {
    $reader = New-Object System.IO.BinaryReader([System.Console]::OpenStandardInput())
    $length = $reader.ReadInt32()
    $messageRaw = [System.Text.Encoding]::UTF8.GetString($reader.ReadBytes($length))
    $message = $messageRaw | ConvertFrom-Json

    if ($message.Status -eq "validate") {
        if (-not (Test-Path $jabRefJar)) {
            return Respond @{message="jarNotFound";path=$jabRefJar}
        } else {
            return Respond @{message="jarFound"}
        }
    }
    
    if (-not (Test-Path $jabRefJar)) {

        $wshell = New-Object -ComObject Wscript.Shell
        $popup = "Unable to locate '$jabRefJarFileName' in '$([System.IO.Path]::GetDirectoryName($jabRefJar))'."
        $wshell.Popup($popup,0,"JabRef", 0x0 + 0x30)
        return
    }

    javaw -jar $jabRefJar -importBibtex $message.Text
} finally {
    $reader.Dispose()
}
