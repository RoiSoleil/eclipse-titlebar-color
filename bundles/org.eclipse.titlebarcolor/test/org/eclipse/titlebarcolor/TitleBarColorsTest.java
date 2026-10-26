package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleBarColorsTest {

	@BeforeEach
	void clean() {
		TitleBarColors.useForTests(new EclipsePreferences());
		TitleBarColors.set(null);
	}

	@Test
	void parsesAndFormatsTheColor() {
		assertEquals(new RGB(0x1F, 0x6F, 0xB5), TitleBarColors.parse("#1F6FB5"));
		assertEquals(new RGB(0x1F, 0x6F, 0xB5), TitleBarColors.parse(" 1f6fb5 "));
		assertEquals("#1F6FB5", TitleBarColors.format(new RGB(0x1F, 0x6F, 0xB5)));
		assertEquals("#000A0B", TitleBarColors.format(new RGB(0, 10, 11)));
	}

	@Test
	void refusesWhatIsNotAColor() {
		assertNull(TitleBarColors.parse(null));
		assertNull(TitleBarColors.parse(""));
		assertNull(TitleBarColors.parse("#12345"));
		assertNull(TitleBarColors.parse("#GG0000"));
		assertNull(TitleBarColors.parse("red"));
	}

	@Test
	void keepsTheColorOfTheWorkspace() {
		assertNull(TitleBarColors.get());
		TitleBarColors.set(new RGB(200, 30, 20));
		assertEquals(new RGB(200, 30, 20), TitleBarColors.get());
		TitleBarColors.set(null);
		assertNull(TitleBarColors.get());
	}

	@Test
	void readsAGarbledPreferenceAsNoColor() {
		TitleBarColors.preferences().put(TitleBarColors.COLOR, "not a color");
		assertNull(TitleBarColors.get());
	}

	@Test
	void picksTheTextThatReadsOnTheBackground() {
		assertTrue(TitleBarColors.prefersLightText(new RGB(0, 0, 0)));
		assertTrue(TitleBarColors.prefersLightText(new RGB(0xC0, 0x39, 0x2B)));
		assertFalse(TitleBarColors.prefersLightText(new RGB(255, 255, 255)));
		assertFalse(TitleBarColors.prefersLightText(new RGB(255, 220, 0)));
	}
}
