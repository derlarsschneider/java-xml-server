<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
			<table border="1">
			<h1>CD Catalog</h1>
				<xsl:for-each select="CATALOG/CD">
					<tr>
						<td>
							<xsl:value-of select="TITLE" />
						</td>
					</tr>
				</xsl:for-each>
				<sql-select>select title from cd</sql-select>
			</table>
			</body>
		</html>


	</xsl:template>

</xsl:stylesheet>