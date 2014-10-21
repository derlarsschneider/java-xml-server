<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml"/>
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
							<xsl:value-of select="ARTIST" />
						</td>
						<td>
							<xsl:value-of select="TITLE" />
						</td>
						<td>
							<xsl:value-of select="COUNTRY" />
						</td>
						<td>
							<xsl:value-of select="COMPANY" />
						</td>
						<td>
							<xsl:value-of select="PRICE" />
						</td>
						<td>
							<xsl:value-of select="YEAR" />
						</td>
					</tr>
					<sql-select>
						select ARTIST,TITLE,country,company,price,year from CD where 
						ARTIST LIKE 'encode(<xsl:value-of select="ARTIST" />)'
						AND TITLE LIKE 'encode(<xsl:value-of select="TITLE" />)'
						AND COMPANY LIKE 'encode(<xsl:value-of select="COMPANY" />)'
						AND YEAR LIKE 'encode(<xsl:value-of select="YEAR" />)'
					</sql-select>
				</xsl:for-each>
			</table>
			</body>
		</html>


	</xsl:template>

</xsl:stylesheet>
