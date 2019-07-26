package datasources;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import querybuilder.SelectBuilder;

public class Oracle {

	private HikariDataSource datasource;
	private HikariConfig config;
	private SelectBuilder builder;
	private String rangeQuery = null;

	public Oracle() {
	};

	public Oracle open(final String host, final String port, final String databaseName, final String user,
			final String password) throws SQLException {
		final String url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + databaseName;
		System.out.println(" URL " + url);
		if (datasource == null) {
			config = new HikariConfig();
			config.setJdbcUrl(url);
			config.setUsername(user);
			config.setPassword(password);
		}
		return this;
	}

	public Oracle open(final String url, final String user, final String password) {
		if (datasource == null) {
			config = new HikariConfig();
			config.setJdbcUrl(url);
			config.setUsername(user);
			config.setPassword(password);
		}
		return this;
	}

	public Oracle initPool(final int maxPoolSize, final boolean autoCommit) {
		config.setMaximumPoolSize(maxPoolSize);
		config.setAutoCommit(autoCommit);
		return this;
	}

	public HikariDataSource build() {
		datasource = new HikariDataSource(config);
		return datasource;
	}

	public Oracle cachePrepared(final boolean isCacheable) {
		config.addDataSourceProperty("cachePrepStmts", isCacheable);
		return this;
	}

	public Oracle maxIdleTimeout(final int idleTimeoutSize) {
		config.setIdleTimeout(idleTimeoutSize);
		return this;
	}

	public Oracle preparedCacheLimit(final int size) {
		config.addDataSourceProperty("prepStmtCacheSqlLimit", size);
		return this;
	}

	public Map<String, String> getDatabaseMetaData() {
		Map<String, String> map = new HashMap<String, String>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				rs = metadata.getTableTypes();
				final List<String> typeList = new ArrayList<String>();
				while (rs.next()) {
					String type = rs.getString(1);
					typeList.add(type);
				}
				int size = typeList.size();
				String[] types = new String[size];
				for (int i = 0; i < size; i++) {
					types[i] = typeList.get(i);
				}
				map = getMetaData(types);
			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}
		}
		return map;
	}

	public String buildColumData() throws SQLException {
		final String[] type = { "TABLE" };
		final List<String> tables = getListOfTables(type);
		final String json = getColumMetaDeta(tables);
		return json;
	}
	
	public List<String> getListOfTables(String schemaPattern) {
		String[] type = { "TABLE" };
		final List<String> tables = new ArrayList<String>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				final String catalog = connection.getCatalog();
				rs = metadata.getTables(catalog, schemaPattern, "%", type);
				while (rs.next()) {
					String name = rs.getString(3);
					tables.add(name);
					System.out.println("Table Name " + name);
				}

			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}
		}
		System.out.println(" TABLE LIST " + tables);
		return tables;
	}

	public List<String> getListOfTables() {
		String[] type = { "TABLE" };
		final List<String> tables = new ArrayList<String>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				final String catalog = connection.getCatalog();
				rs = metadata.getTables(catalog, null, "%", type);
				while (rs.next()) {
					String name = rs.getString(3);
					tables.add(name);
					System.out.println("Table Name " + name);
				}

			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}
		}
		System.out.println(" TABLE LIST " + tables);
		return tables;
	}

	public void shutdown() {
		if (!this.datasource.isClosed()) {
			this.datasource.close();
			System.out.println(" Closing datasource...");
		}
		this.datasource = null;
	}

	public Oracle select() {
		builder = new SelectBuilder();
		return this;
	}

	public Oracle columns(List<String> columns) {
		for (String column : columns) {
			builder.column(column);
		}
		return this;
	}

	public Oracle columns(String[] columns) {
		int size = columns.length;
		for (int i = 0; i < size; i++) {
			builder.column(columns[i]);
		}
		return this;
	}

	public Oracle allColumns(String all) {
		builder.column(all);
		return this;
	}

	public Oracle from(String table) {
		builder.from(table);
		return this;
	}

	public Oracle where(String expression) {
		builder.where(expression);
		return this;
	}

	public Oracle orderBy(String columnName, boolean sortOrder) {
		builder.orderBy(columnName, sortOrder);
		return this;
	}

	public Oracle groupBy(String expression) {
		builder.groupBy(expression);
		return this;
	}

	public Oracle distinct() {
		builder.distinct();
		return this;
	}

	public Oracle range(int from, int to) {
		this.rangeQuery = " offset " + from + " rows fetch next " + to + " rows only ";
		return this;
	}

	public String buildQuery() {
		String generatedQuery = null;
		if (this.rangeQuery != null) {
			generatedQuery = this.builder.build() + this.rangeQuery;
		} else {
			generatedQuery = this.builder.build();
		}
		return generatedQuery;
	}

	public boolean testConnection() {
		boolean isConnected = false;
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = this.datasource.getConnection();
			ps = connection.prepareStatement("SELECT 1 FROM DUAL");
			isConnected = ps.execute();
		} catch (SQLException e) {
			close(connection, ps, null);
			e.printStackTrace();
		} finally {
			close(connection, ps, null);
		}
		return isConnected;
	}

	public boolean execute(String query) {
		boolean isConnected = false;
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = this.datasource.getConnection();
			ps = connection.prepareStatement(query);
			isConnected = ps.execute();
		} catch (SQLException e) {
			close(connection, ps, null);
			e.printStackTrace();
		} finally {
			close(connection, ps, null);
		}
		return isConnected;
	}

	public String getColumnMetaData(String table) {
		final List<ColumnMetaData> listOfColumnData = new ArrayList<ColumnMetaData>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				rs = metadata.getColumns(null, null, table, null);
				while (rs.next()) {
					ColumnMetaData columnMetaData = new ColumnMetaData();
					columnMetaData.setColumnName(rs.getString("COLUMN_NAME"));
					columnMetaData.setTypeName(rs.getString("TYPE_NAME"));
					columnMetaData.setColumnSize(rs.getString("COLUMN_SIZE"));
					listOfColumnData.add(columnMetaData);
				}
			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}
		}
		Gson gson = new Gson();
		return gson.toJson(listOfColumnData);
	}

	private List<String> getListOfTables(String[] types) {
		final List<String> tables = new ArrayList<String>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				final String catalog = connection.getCatalog();
				rs = metadata.getTables(catalog, null, "%", types);
				while (rs.next()) {
					String name = rs.getString(3);
					tables.add(name);
					System.out.println("Table Name " + name);
				}

			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}
		}
		System.out.println(" TABLE LIST " + tables);
		return tables;
	}

	private String getColumMetaDeta(List<String> tables) {
		final List<ColumnMetaData> listOfColumnData = new ArrayList<ColumnMetaData>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				System.out.println("SIZE " + tables.size());
				for (String table : tables) {
					rs = metadata.getColumns(null, null, table, null);
					while (rs.next()) {
						ColumnMetaData columnMetaData = new ColumnMetaData();
						columnMetaData.setColumnName(rs.getString("COLUMN_NAME"));
						columnMetaData.setTypeName(rs.getString("TYPE_NAME"));
						columnMetaData.setColumnSize(rs.getString("COLUMN_SIZE"));
						listOfColumnData.add(columnMetaData);
					}
				}
			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}
		}
		Gson gson = new Gson();
		return gson.toJson(listOfColumnData);
	}

	private Map<String, String> getMetaData(String[] types) {
		final Map<String, String> map = new HashMap<String, String>();
		if (datasource != null) {
			Connection connection = null;
			ResultSet rs = null;
			try {
				connection = datasource.getConnection();
				final DatabaseMetaData metadata = connection.getMetaData();
				final String catalog = connection.getCatalog();
				rs = metadata.getTables(catalog, null, "%", types);
				while (rs.next()) {
					String name = rs.getString(3);
					String type = rs.getString(4);
					map.put(name, type);
				}
			} catch (SQLException e) {
				close(connection, null, rs);
				e.printStackTrace();
			} finally {
				close(connection, null, rs);
			}

			System.out.println(" TYPES : " + types + " MAP : " + map);
		}
		return map;
	}

	/*
	 * closes result set, prepared statement and connection. connection is returned
	 * to pool on closing
	 */
	private void close(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
		try {
			if (resultSet != null) {
				if (!resultSet.isClosed()) {
					resultSet.close();
				}
			}
			if (preparedStatement != null) {
				if (!preparedStatement.isClosed()) {
					preparedStatement.close();
				}
			}
			if (connection != null) {
				if (!connection.isClosed()) {
					connection.close();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
