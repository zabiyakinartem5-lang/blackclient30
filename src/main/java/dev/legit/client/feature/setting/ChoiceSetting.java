package dev.legit.client.feature.setting;

/** Выбор из списка вариантов. ЛКМ - следующий, ПКМ - предыдущий. */
public class ChoiceSetting extends Setting {
	public final String[] options;
	public int index;

	public ChoiceSetting(String name, int index, String... options) {
		super(name);
		this.options = options;
		this.index = Math.max(0, Math.min(options.length - 1, index));
	}

	public String get() {
		return options[index];
	}

	public void next() {
		index = (index + 1) % options.length;
	}

	public void prev() {
		index = (index - 1 + options.length) % options.length;
	}

	public void set(int i) {
		index = Math.max(0, Math.min(options.length - 1, i));
	}

	@Override
	public int height() {
		return 17;
	}
}
