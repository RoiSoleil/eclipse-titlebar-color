-- Answers "Don't Allow" to the alerts of macOS in front of Eclipse (local network...), for tools/screenshot.sh.
-- Needs the accessibility permission, which the runners of GitHub give: elsewhere it does nothing.
tell application "System Events"
	repeat with processName in {"UserNotificationCenter", "CoreServicesUIAgent", "SecurityAgent", "Eclipse"}
		try
			tell process processName
				repeat with w in windows
					try
						click button "Don't Allow" of w
					end try
				end repeat
			end tell
		end try
	end repeat
end tell
