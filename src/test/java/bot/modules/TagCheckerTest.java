package bot.modules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TagCheckerTest {
	@Test
	void wholesomeCheckCheck() {
		assertTrue(TagChecker.wholesomeCheck(new ArrayList<>()));
		assertFalse(TagChecker.wholesomeCheck(new ArrayList<>(Arrays.asList("a", "b", "lolicon"))));
		assertFalse(TagChecker.wholesomeCheck(new ArrayList<>(Arrays.asList("necrophilia", "eye penetration"))));
		assertTrue(TagChecker.wholesomeCheck(new ArrayList<>(Arrays.asList("futanari", "your mom", "your dad")), "futanari"));
	}

	@Test
	void tagCheckCheck() {
		assertEquals(1, TagChecker.tagCheckWithWarnings(new ArrayList<>(Arrays.asList("futanari", "your mom", "your dad"))).size());
		assertEquals(3, TagChecker.tagCheckWithWarnings(new ArrayList<>(Arrays.asList("incest", "shemale", "rape"))).size());
		assertEquals(1, TagChecker.tagCheck(new ArrayList<>(Arrays.asList("incest", "shemale", "rape"))).size());
	}
}
