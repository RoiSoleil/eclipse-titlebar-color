# Places the Eclipse window and saves it, title bar included, as a PNG. Called by tools/screenshot.sh.
param([string]$Out, [int]$Wait = 300, [int]$Settle = 40)
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System; using System.Text; using System.Runtime.InteropServices;
public struct RECT { public int Left, Top, Right, Bottom; }
public class W {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc p, IntPtr l);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
  [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int attr, out RECT r, int size);
}
"@
# The bounds of DWM are in physical pixels: so are the coordinates of the screen copy once the process says so.
[void][W]::SetProcessDPIAware()
# The workbench is the largest visible window titled "<workspace> - Eclipse IDE": a dialog may share the name.
function Find-Workbench {
  $script:found = [IntPtr]::Zero; $script:area = 0
  [W]::EnumWindows({
    param($h, $l)
    if ([W]::IsWindowVisible($h)) {
      $sb = New-Object System.Text.StringBuilder 512
      [void][W]::GetWindowText($h, $sb, 512)
      if ($sb.ToString() -like '* - Eclipse IDE') {
        $r = New-Object RECT
        [void][W]::GetWindowRect($h, [ref]$r)
        $a = ($r.Right - $r.Left) * ($r.Bottom - $r.Top)
        if ($a -gt $script:area) { $script:found = $h; $script:area = $a }
      }
    }
    return $true
  }, [IntPtr]::Zero) | Out-Null
  return $script:found
}
$deadline = (Get-Date).AddSeconds($Wait)
do { Start-Sleep -Seconds 5; $found = Find-Workbench } while ($found -eq [IntPtr]::Zero -and (Get-Date) -lt $deadline)
if ($found -eq [IntPtr]::Zero) { Write-Error 'No Eclipse window'; exit 1 }
Start-Sleep -Seconds $Settle
$found = Find-Workbench
[void][W]::ShowWindow($found, 9)
[void][W]::SetWindowPos($found, [IntPtr]::Zero, 20, 20, 1280, 800, 0x0040)
[void][W]::SetForegroundWindow($found)
Start-Sleep -Seconds 5
# The bounds drawn on the screen, without the invisible resize borders of Windows 10 and 11.
$r = New-Object RECT
[void][W]::DwmGetWindowAttribute($found, 9, [ref]$r, 16)
$w = $r.Right - $r.Left; $h = $r.Bottom - $r.Top
$bitmap = New-Object System.Drawing.Bitmap $w, $h
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.CopyFromScreen($r.Left, $r.Top, 0, 0, $bitmap.Size)
$bitmap.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output "Saved $w x $h to $Out"
