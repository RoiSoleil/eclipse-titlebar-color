# Saves the window of the workbench, title bar included, as a PNG. Called by tools/screenshot.sh once the window
# is ready.
param([string]$Out, [string]$Title = '* - Eclipse IDE')
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
# The largest visible window with this title: "<workspace> - Eclipse IDE" for the workbench, which a dialog may share,
# or that of a dialog.
function Find-Workbench {
  $script:found = [IntPtr]::Zero; $script:area = 0
  [W]::EnumWindows({
    param($h, $l)
    if ([W]::IsWindowVisible($h)) {
      $sb = New-Object System.Text.StringBuilder 512
      [void][W]::GetWindowText($h, $sb, 512)
      if ($sb.ToString() -like $Title) {
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
$found = Find-Workbench
if ($found -eq [IntPtr]::Zero) { Write-Error 'No Eclipse window'; exit 1 }
[void][W]::ShowWindow($found, 9)
# In front of every other window: Windows does not let a background process take the focus.
[void][W]::SetWindowPos($found, [IntPtr](-1), 0, 0, 0, 0, 0x0043)
[void][W]::SetForegroundWindow($found)
Start-Sleep -Seconds 3
# The bounds drawn on the screen, without the invisible resize borders of Windows 10 and 11.
$r = New-Object RECT
[void][W]::DwmGetWindowAttribute($found, 9, [ref]$r, 16)
$w = $r.Right - $r.Left; $h = $r.Bottom - $r.Top
$bitmap = New-Object System.Drawing.Bitmap $w, $h
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.CopyFromScreen($r.Left, $r.Top, 0, 0, $bitmap.Size)
$bitmap.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output "Saved $w x $h to $Out"
