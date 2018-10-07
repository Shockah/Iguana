package pl.shockah.iguana.format;

public interface Formatter<ParseContext, OutputContext, Output> extends FormattingParser<ParseContext>, FormattingOutputer<OutputContext, Output> {
}