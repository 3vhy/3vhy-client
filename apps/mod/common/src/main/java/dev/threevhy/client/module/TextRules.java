package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.Config.TextRule;
import dev.threevhy.client.Hooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Name Protect's text changes: anything on screen matching a rule shows as something else, e.g.
 * "Money: #" becomes "Money: 1T". # matches any number (500.9T, 1,234, 5.1K) and * any word.
 * Single pieces of text are changed where Minecraft draws text (see ProtectedText); scoreboard lines are
 * changed whole, because servers often colour the label and the number as separate pieces.
 */
public final class TextRules {
	private static final String NUMBER = "-?\\d[\\d.,]*[kKmMbBtTqQ]?";
	private static final String WORD = "\\S+";

	/** One rule ready to use. {@code literal} is the text before the first # or *, for a quick check. */
	private record Rule(Pattern pattern, String literal, String replacement, int keep) {
	}

	private static volatile List<Rule> rules = List.of();
	private static String builtFrom = "";

	private TextRules() {
	}

	/** Recompiles the rules when the player changed them. Called every tick. */
	static void tick() {
		List<TextRule> source = Config.get().textRules;
		StringBuilder signature = new StringBuilder();
		for (TextRule rule : source) signature.append(rule.find).append('\u0000').append(rule.replace).append('\u0001');
		String now = signature.toString();
		if (now.equals(builtFrom)) return;
		builtFrom = now;
		List<Rule> compiled = new ArrayList<>();
		for (TextRule rule : source) {
			if (rule.find == null || rule.find.isBlank()) continue;
			compiled.add(compile(rule.find, rule.replace == null ? "" : rule.replace));
		}
		rules = List.copyOf(compiled);
	}

	static Rule compile(String find, String replace) {
		StringBuilder regex = new StringBuilder();
		StringBuilder literal = new StringBuilder();
		StringBuilder prefix = new StringBuilder();
		boolean wildcardSeen = false;
		for (char c : find.toCharArray()) {
			if (c == '#' || c == '*') {
				if (!literal.isEmpty()) regex.append(Pattern.quote(literal.toString()));
				literal.setLength(0);
				regex.append(c == '#' ? NUMBER : WORD);
				wildcardSeen = true;
			} else {
				literal.append(c);
				if (!wildcardSeen) prefix.append(c);
			}
		}
		if (!literal.isEmpty()) regex.append(Pattern.quote(literal.toString()));
		int keep = 0;
		while (keep < prefix.length() && keep < replace.length() && prefix.charAt(keep) == replace.charAt(keep)) keep++;
		return new Rule(Pattern.compile(regex.toString()), prefix.toString(), replace, keep);
	}

	public static boolean active() {
		return Modules.NAME_PROTECT.enabled() && !rules.isEmpty();
	}

	/** Applies every rule to the text so far ({@code original} when {@code rewrite} is still null). */
	static ProtectedText.Rewrite rewrite(ProtectedText.Rewrite rewrite, String original) {
		if (!active()) return rewrite;
		for (Rule rule : rules) {
			String text = rewrite == null ? original : rewrite.text;
			if (!rule.literal().isEmpty() && !text.contains(rule.literal())) continue;
			Matcher matcher = rule.pattern().matcher(text);
			ProtectedText.Edits edits = null;
			String replacement = rule.replacement();
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				if (end == start) continue;
				if (end - start == replacement.length() && text.startsWith(replacement, start)) continue;
				if (edits == null) edits = new ProtectedText.Edits();
				edits.add(start, end, replacement);
			}
			if (edits == null) continue;
			Hooks.textRuleRan = true;
			if (rewrite == null) rewrite = new ProtectedText.Rewrite(original);
			rewrite.apply(edits);
		}
		return rewrite;
	}

	/**
	 * Applies the rules to a whole line made of differently styled pieces. The part a rule keeps (like
	 * "Money: ") keeps its colours, and the new value takes the colour of the value it replaces.
	 */
	public static Component apply(Component line) {
		if (line == null || !active()) return line;
		StringBuilder joined = new StringBuilder();
		List<Style> styles = new ArrayList<>();
		line.visit((style, part) -> {
			joined.append(part);
			for (int i = 0; i < part.length(); i++) styles.add(style);
			return Optional.empty();
		}, Style.EMPTY);
		String text = joined.toString();
		boolean changed = false;
		for (Rule rule : rules) {
			if (!rule.literal().isEmpty() && !text.contains(rule.literal())) continue;
			Matcher matcher = rule.pattern().matcher(text);
			if (!matcher.find()) continue;
			StringBuilder out = new StringBuilder();
			List<Style> outStyles = new ArrayList<>();
			int last = 0;
			do {
				copy(text, styles, last, matcher.start(), out, outStyles);
				int keep = Math.min(rule.keep(), matcher.end() - matcher.start());
				copy(text, styles, matcher.start(), matcher.start() + keep, out, outStyles);
				Style valueStyle = styles.isEmpty() ? Style.EMPTY
						: styles.get(Math.min(styles.size() - 1, Math.min(matcher.start() + keep, matcher.end() - 1)));
				String value = rule.replacement().substring(Math.min(keep, rule.replacement().length()));
				out.append(value);
				for (int i = 0; i < value.length(); i++) outStyles.add(valueStyle);
				last = matcher.end();
			} while (matcher.find());
			copy(text, styles, last, text.length(), out, outStyles);
			text = out.toString();
			styles.clear();
			styles.addAll(outStyles);
			changed = true;
		}
		if (!changed) return line;
		Hooks.textRuleRan = true;
		MutableComponent result = Component.empty();
		int start = 0;
		for (int i = 1; i <= text.length(); i++) {
			if (i == text.length() || !styles.get(i).equals(styles.get(start))) {
				result.append(Component.literal(text.substring(start, i)).withStyle(styles.get(start)));
				start = i;
			}
		}
		return result;
	}

	private static void copy(String text, List<Style> styles, int from, int to, StringBuilder out, List<Style> outStyles) {
		out.append(text, from, to);
		outStyles.addAll(styles.subList(from, to));
	}

	public static boolean add(String find, String replace) {
		String clean = find.trim();
		if (clean.isEmpty()) return false;
		TextRule rule = new TextRule();
		rule.find = clean;
		rule.replace = replace;
		Config.get().textRules.add(rule);
		Config.saveSoon();
		tick();
		return true;
	}

	public static void remove(TextRule rule) {
		Config.get().textRules.remove(rule);
		Config.saveSoon();
		tick();
	}
}
