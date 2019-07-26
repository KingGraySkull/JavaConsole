package querybuilder;

import java.util.List;

public abstract class AbstractSqlBuilder {

	protected void appendList(StringBuilder sql, List<?> list, String init, String sep) {

		boolean first = true;

		for (Object s : list) {
			if (first) {
				sql.append(init);
			} else {
				sql.append(sep);
			}
			sql.append(s);
			first = false;
		}
	}
	
	protected void appendRange(StringBuilder sql, final String vendorName,int from, int to) {
		
		String rangeQuery = "";
		if(vendorName.equalsIgnoreCase("oracle")) {
			rangeQuery = " offset "+from+ " rows fetch next "+to+" rows only ";
		}
		
		sql.append(rangeQuery);
	}
}
