<!--Silk to LIMES Linkspec Transformation Version 0.1 by Konrad HÃ¶ffner 2011 konrad.hoeffner@uni-leipzig.deImplementation Limitations:- the source dataset in the Silk LSL must refer to the first data source and the target dataset to the second one- only one interlink element per Silk LSL is allowed- data source properties may occur multiple times (I'm not sure if that is problematic or not)newlines are made by "<xsl:value-of select="$newline"/>"Inherent Limitations:- in LIMES you can have only one output format for all output files, because of this, the first one in Silk is chosen for all in LIMES- in Silk you can only have one relation per LinkType while in LIMES it is per file. Because of this, the first link types relation is used for all files in LIMES.-->
<xsl:stylesheet version = '2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns:exslt="http://exslt.org/common">
      <xsl:output method="xml" version="1.0" encoding="UTF-8" omit-xml-declaration="no" standalone="no" doctype-system="limes.dtd"
		  cdata-section-elements="namelist" indent="yes" media-type="text/xml"/>
      <xsl:variable name="newline"><xsl:text>&#xa;</xsl:text></xsl:variable>
      <xsl:template match="/Silk">
            <LIMES><xsl:value-of select="$newline"/>
                  <xsl:for-each select="Prefixes/Prefix">
                        <PREFIX>
                             <NAMESPACE>
                                    <xsl:value-of select="@namespace"/>
                              </NAMESPACE>
                              <LABEL>
                                    <xsl:value-of select="@id"/>
                              </LABEL>
                        </PREFIX>
                        <xsl:value-of select="$newline"/>
                  </xsl:for-each>
		 <xsl:variable name="sourceDataset" select="/Silk/Interlinks/Interlink[1]/SourceDataset[1]"/>
		 <xsl:variable name="targetDataset" select="/Silk/Interlinks/Interlink[1]/TargetDataset[1]"/>
		 <xsl:variable name="sourceID" select="$sourceDataset/@dataSource"/>
		 <xsl:variable name="targetID" select="$targetDataset/@dataSource"/>
		 <xsl:variable name="sourceVariable" select="$sourceDataset/@var"/>
		 <xsl:variable name="targetVariable" select="$targetDataset/@var"/>
		 <xsl:variable name="fullProperties" select="//Input/@path" />
                              <SOURCE>
                               <xsl:for-each select="DataSources/DataSource[@id=$sourceID][1]">
                                <xsl:call-template name="Datasource"/>
                               </xsl:for-each>
                               <xsl:for-each select="/Silk/Interlinks/Interlink[1]/SourceDataset[1]">
                                <xsl:call-template name="Dataset"/>
                               </xsl:for-each>
		                  <xsl:call-template name="getProperties">
                	          <xsl:with-param name="variable" select="$sourceVariable"/>
	                          <xsl:with-param name="fullProperties" select="$fullProperties"/>
          	 	       </xsl:call-template> 		      
                              </SOURCE>
                              <TARGET>
                               <xsl:for-each select="DataSources/DataSource[@id=$targetID][1]">
                                <xsl:call-template name="Datasource"/>
                               </xsl:for-each>
                               <xsl:for-each select="/Silk/Interlinks/Interlink[1]/TargetDataset[1]">
                                <xsl:call-template name="Dataset"/>
                               </xsl:for-each>
		                  <xsl:call-template name="getProperties">
                	          <xsl:with-param name="variable" select="$targetVariable"/>
	                          <xsl:with-param name="fullProperties" select="$fullProperties"/>
          	 	       </xsl:call-template> 		      
                              </TARGET> 
                  <METRIC>
                        <xsl:for-each select="Interlinks/Interlink/LinkCondition[1]">
                              <xsl:apply-templates select="Aggregate"/>
                        </xsl:for-each>
                  </METRIC>
                  <xsl:value-of select="$newline"/>
                  <xsl:for-each select="Interlinks/Interlink/Outputs">
                        <xsl:apply-templates select="Output"/>
                  </xsl:for-each>
                  <EXECUTION>Simple</EXECUTION>
                  <xsl:value-of select="$newline"/>
                  <OUTPUT>
                        <xsl:for-each select="Interlinks/Interlink/Outputs/Output[1]/Param[@name='format']">
                              <xsl:choose>
                                    <xsl:when test="@value='ntriples'">N3</xsl:when>
                                    <xsl:when test="@value='alignment'">TAB</xsl:when>
                                    <xsl:otherwise>TAB</xsl:otherwise>
                              </xsl:choose>
                        </xsl:for-each>
                  </OUTPUT>
                  <xsl:value-of select="$newline"/>
            </LIMES>
      </xsl:template>

      <xsl:template name="Datasource">
<!--            <xsl:param name="id"/> -->
            <!-- id and endpoint must be always there both in Silk and in LIMES-->
<!--            <ID>
                  <xsl:value-of select="@id"/>
            </ID>
            <xsl:value-of select="$newline"/>-->
            <ENDPOINT>
                  <xsl:value-of select="Param[@name='endpointURI']/@value"/>
            </ENDPOINT>
            <xsl:value-of select="$newline"/>
            <!-- page size may not be existent in Silk (and is then defaulted to be 1000) but must always be specified in LIMES. To increase compatibility, page size in Limes is set to 1000 if unset in Silk. -1 for "no maximum page size" may also be used in LIMES, however. -->
            <PAGESIZE>
                  <xsl:choose>
                        <xsl:when test="Param[@name='pageSize']">
                              <xsl:value-of select="Param[@name='pageSize']/@value"/>
                        </xsl:when>
                        <xsl:otherwise>1000</xsl:otherwise>
                  </xsl:choose>
            </PAGESIZE>
            <xsl:value-of select="$newline"/>
            <!-- the graph is optional both in Silk and in LIME-->
            <xsl:if test="Param[@name='graph']">
                  <GRAPH>
                        <xsl:value-of select="Param[@name='graph']/@value"/>
                  </GRAPH>
                  <xsl:value-of select="$newline"/>
            </xsl:if>
             <!-- in both the type is optional and defaults to a sparql endpoint-->
            <xsl:if test="@type">
                  <TYPE>
            	 	 <xsl:choose>
			<!-- I don't know the other silk types at the moment -->
                        	<xsl:when test="@type='sparqlEndpoint'">
                              		<xsl:text>sparql</xsl:text>
	                        </xsl:when>
                        <xsl:otherwise>sparql</xsl:otherwise>
        	          </xsl:choose>
                  </TYPE>
            </xsl:if>
           </xsl:template>

      <xsl:template name="Aggregate" match="Aggregate">
            <xsl:param name="factor"/>
            <xsl:choose>
                  <xsl:when test="$factor!=''">
                        <xsl:value-of select='$factor'/>*</xsl:when>
            </xsl:choose>
            <!-- XSLT is a bit silly for stuff like this, if you know a better method to do conditional variable setting, please tell me :-)-->
            <xsl:variable name="newFactor">
                  <xsl:if test="@type='average'"><xsl:value-of select="1 div (count(Aggregate)+count(Compare))"/></xsl:if>
            </xsl:variable>
            <xsl:choose>
                  <xsl:when test="@type='average'">
                        <xsl:text>ADD</xsl:text>
                  </xsl:when>
                  <xsl:when test="@type='max'">
                        <xsl:text>MAX</xsl:text>
                  </xsl:when>
                  <xsl:when test="@type='min'">
                        <xsl:text>MIN</xsl:text>
                  </xsl:when>
                  <xsl:when test="@type='quadraticMean'">
                        <xsl:text>INSERTHERE</xsl:text>
                  </xsl:when>
                  <xsl:when test="@type='geometricMean'">
                        <xsl:text>INSERTHERE</xsl:text>
                  </xsl:when>
                  <xsl:otherwise>
                        <xsl:text>error, aggregate type "</xsl:text>
                        <xsl:value-of select="@type"/>
                        <xsl:text>" not known</xsl:text>
                  </xsl:otherwise>
            </xsl:choose>
            <xsl:text>(</xsl:text>
            <xsl:for-each select="Aggregate">
                  <xsl:call-template name="Aggregate">
                        <xsl:with-param name="factor" select="$newFactor"/>
                  </xsl:call-template>
                  <xsl:if test="following-sibling::Aggregate">
                        <xsl:text>, </xsl:text>
                  </xsl:if>
                  <xsl:if test="following-sibling::Compare">
                        <xsl:text>, </xsl:text>
                  </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="Compare">
                  <xsl:call-template name="Compare">
                        <xsl:with-param name="factor" select="$newFactor"/>
                  </xsl:call-template>
                  <xsl:if test="following-sibling::Compare">
                        <xsl:text>, </xsl:text>
                  </xsl:if>
            </xsl:for-each>
            <xsl:text>)</xsl:text>
      </xsl:template>

      <xsl:template name="Compare">
            <xsl:param name="factor"/>
            <xsl:choose>
                  <xsl:when test="$factor!=''"> <xsl:value-of select='$factor'/>*</xsl:when>
            </xsl:choose>
            <xsl:choose>
                  <xsl:when test="@metric='wgs84'">
                        <xsl:text>WGS84</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='jaro'">
                        <xsl:text>Jaro</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='qGrams'">
                        <xsl:text>Trigram</xsl:text>
                  </xsl:when>
                  <!-- Silk uses n-grams where the n is specified by the parameter ... but LIMES can only do Trigrams. -->
                  <xsl:when test="@metric='levenshtein'">
                        <xsl:text>Levenshtein</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='jaroWinkler'">
                        <xsl:text>JaroWinkler</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='inequality'">
                        <xsl:text>Inequality</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='equality'">
                        <xsl:text>Equality</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='num'">
                        <xsl:text>Num</xsl:text>
                  </xsl:when>
                  <xsl:when test="@metric='date'">
                        <xsl:text>Date</xsl:text>
                  </xsl:when>
            </xsl:choose>
            <xsl:text>(</xsl:text>
                <xsl:call-template name="Inputs"/>
           <xsl:text>)</xsl:text>
      </xsl:template>
      
<!--      <xsl:template name="TransformInput" match="Input">i
	<xsl:value-of select="@function"/><xsl:text>()</xsl:text>
            <xsl:value-of select="replace(replace(@path,'\?',''),'/','.')"/>
      </xsl:template>-->
      
      <xsl:template name="Inputs">
             <xsl:for-each select="Input|TransformInput">
                  <xsl:call-template name="Input"/>
                  <xsl:if test="following-sibling::TransformInput|following-sibling::Input">
                        <xsl:text>, </xsl:text>
                  </xsl:if>
            </xsl:for-each>
      </xsl:template>
     <!-- either has or is an input. take only the first one it has because limes does not seem to support string concatenation. --> 
      <xsl:template name="Input" >
            <xsl:value-of select="replace(replace(@path,'\?',''),'/','.')"/>
	<xsl:if test="Input">
            <xsl:value-of select="replace(replace(Input[1]/@path,'\?',''),'/','.')"/>
	</xsl:if>
      </xsl:template>

      <xsl:template match="Output">
            <xsl:choose>
                  <xsl:when test="@minConfidence">
                        <ACCEPTANCE>
                              <xsl:value-of select="$newline"/>
                              <xsl:call-template name="OutputInner"/>
                        </ACCEPTANCE>
                        <xsl:value-of select="$newline"/>
                  </xsl:when>
                  <xsl:when test="@maxConfidence">
                        <REVIEW>
                              <xsl:value-of select="$newline"/>
                              <xsl:call-template name="OutputInner"/>
                        </REVIEW>
                        <xsl:value-of select="$newline"/>
                  </xsl:when>
            </xsl:choose>
      </xsl:template>
      <xsl:template name="OutputInner">
            <!--Silk:<Param name="file" value="1303320108589/city/links.nt" /><Param name="format" value="ntriples" />-->
            <THRESHOLD>
                  <xsl:value-of select="@minConfidence"/>
                  <xsl:if test="@maxConfidence">
                   <xsl:value-of select="../../Filter/@threshold"/>
                  </xsl:if>
            </THRESHOLD>
            <xsl:value-of select="$newline"/>
            <xsl:for-each select="Param[@name='file']">
                  <FILE>
                        <xsl:value-of select="@value"/>
                  </FILE>
                  <xsl:value-of select="$newline"/>
            </xsl:for-each>
            <RELATION>
                  <xsl:value-of select="Interlinks/LinkType[1]"/>
            </RELATION>
            <xsl:value-of select="$newline"/>
      </xsl:template>


      <xsl:template name="getProperties">
            <xsl:param name="variable"/>
            <xsl:param name="fullProperties"/>
            <xsl:variable name="properties" as="item()*">
                  <xsl:for-each select="$fullProperties">
                        <xsl:if test="starts-with(.,concat('?',$variable))">
                              <xsl:sequence select="substring(.,4)"/>
                        </xsl:if>
                  </xsl:for-each>
            </xsl:variable>
            <!--<xsl:variable name="unique-properties" select="$properties[not(.=preceding::)]" /> -->
            <!--<xsl:for-each select="distinct-values($properties)">-->
            <xsl:for-each select="distinct-values($properties)">
                  <PROPERTY>
                        <xsl:value-of select="."/>
                  </PROPERTY>
                  <xsl:value-of select="$newline"/>
            </xsl:for-each>
      </xsl:template>
      <xsl:template name="Dataset">
	<!-- the variable is defined in silk without a question mark but one is needed in LIMES-->
            <VAR>?<xsl:value-of select="@var"/></VAR>
            <xsl:value-of select="$newline"/>
            <RESTRICTION>
                  <xsl:value-of select="normalize-space(RestrictTo)"/>
            </RESTRICTION>
            <xsl:value-of select="$newline"/>
      </xsl:template>
</xsl:stylesheet>
