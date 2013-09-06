

public class ScoreFunctionFactory {
	public enum Mode {
		RANDOM,
		KLSUM,
		SUMBASIC,
	}
	public static ScoreFunction getScoreFunction(Mode mode)
	{
		if (mode == Mode.KLSUM) {
			return new KLSumPlusScoreFunction();
		} else if (mode == Mode.SUMBASIC) {
			return new SumBasicScoreFunction();
		} else if (mode == Mode.RANDOM) {
			return new RandomScoreFunction();
		}
		return null;
	}
}