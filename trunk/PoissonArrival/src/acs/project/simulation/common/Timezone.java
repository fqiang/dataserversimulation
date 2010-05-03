package acs.project.simulation.common;

public enum Timezone {
	GMT_0(0),
	GMT_1(1),
	GMT_2(2),
	GMT_3(3),
	GMT_4(4),
	GMT_5(5),
	GMT_6(6),
	GMT_7(7),
	GMT_8(8),
	GMT_9(9),
	GMT_10(10),
	GMT_11(11),
	GMT_12(12),

	GMT_N1(-1),
	GMT_N2(-2),
	GMT_N3(-3),
	GMT_N4(-4),
	GMT_N5(-5),
	GMT_N6(-6),
	GMT_N7(-7),
	GMT_N8(-8),
	GMT_N9(-9),
	GMT_N10(-10),
	GMT_N11(-11),
	GMT_N12(-12);
	
	private final int gmt_value;
	
	Timezone(int gmt_value)
	{
		this.gmt_value = gmt_value;
	}
	
	public int getGMT()
	{
		return gmt_value;
	}
}
