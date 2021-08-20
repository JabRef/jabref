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

$jabRefExe = [System.IO.Path]::Combine($PSScriptRoot, "runtime\\bin\\JabRef.bat")

try {
    $reader = New-Object System.IO.BinaryReader([System.Console]::OpenStandardInput())
    $length = $reader.ReadInt32()
    $messageRaw = [System.Text.Encoding]::UTF8.GetString($reader.ReadBytes($length))
    $message = $messageRaw | ConvertFrom-Json

    if ($message.Status -eq "validate") {
        if (-not (Test-Path $jabRefExe)) {
            return Respond @{message="jarNotFound";path=$jabRefExe}
        } else {
            return Respond @{message="jarFound"}
        }
    }

    if (-not (Test-Path $jabRefExe)) {
        $wshell = New-Object -ComObject Wscript.Shell
        $popup = "Unable to locate '$jabRefExe'."
        $wshell.Popup($popup,0,"JabRef", 0x0 + 0x30)
        return
    }

    #$wshell = New-Object -ComObject Wscript.Shell
    #$wshell.Popup($message.Text,0,"JabRef", 0x0 + 0x30)

    $messageText = $message.Text.replace("`n"," ").replace("`r"," ")
    $tempfile = New-TemporaryFile
    # WriteAllLines should write the file as UTF-8 without BOM
    # unlike Out-File which writes UTF-16 with BOM in ps5.1
    [IO.File]::WriteAllLines($tempfile, $messageText)
    $output = & $jabRefExe -importToOpen $tempfile *>&1
    Remove-Item $tempfile
    #$output = "$messageText"
    #$wshell = New-Object -ComObject Wscript.Shell
    #$wshell.Popup($output,0,"JabRef", 0x0 + 0x30)
    return Respond @{message="ok";output="$output"}
} finally {
    $reader.Dispose()
}
