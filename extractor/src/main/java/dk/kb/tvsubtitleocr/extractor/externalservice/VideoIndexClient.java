package dk.kb.tvsubtitleocr.extractor.externalservice;
import dk.kb.tvsubtitleocr.extractor.model.VideoInfo;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VideoIndexClient implements AutoCloseable {
    final static Logger log = LoggerFactory.getLogger(VideoIndexClient.class);
    private String serverUrl;
    private String baseQuery;
    private String withSRT;

    private String withoutSRT;
    private HttpSolrServer connector;

    public VideoIndexClient(String serverUrl) {
        this.serverUrl = serverUrl;
        //TODO Also get recordID's from preservica index. Not needed for now.
        this.baseQuery = " recordID:doms_radioTVCollection* AND lma_long:tv ";
        this.withSRT =    " +srt:* ";
        this.withoutSRT = " -srt:* ";
        this.connector = createConnector(this.serverUrl);
    }

    /**
     *
     * @param startRow How fair into the index you want to start.
     * @param rowCount Total amount of rows to retrieve. Don't go over 1000!
     * @return A list of videos that exist in the index without srt content.
     */
    public List<VideoInfo> getVideosWithoutSRT(int startRow, int rowCount) {
        String query = baseQuery + withoutSRT;
        return getVideos(query, startRow, rowCount);
    }

    /**
     *
     * @param startRow How fair into the index you want to start.
     * @param rowCount Total amount of rows to retrieve. Don't go over 1000!
     * @return A list of videos that exist in the index without srt content.
     */
    public List<VideoInfo> getVideosWithSRT(int startRow, int rowCount) {
        String query = baseQuery + withSRT;
        return getVideos(query, startRow, rowCount);
    }

    public List<VideoInfo> getVideos(String query, int startRow, int rowCount) {
        List<VideoInfo> result = null;

        QueryResponse queryResponse = querySolr(connector, query, startRow, rowCount);
        if(queryResponse != null){
            result = processQueryResponse(queryResponse);
        }

        return result;
    }

    public long getNumVideosWithoutSrt() {
        return getNumFound(getBaseQuery() + getWithoutSRT());
    }

    public long getNumVideosWithSrt() {
        return getNumFound(getBaseQuery() + getWithSRT());
    }


    protected long getNumFound(String queryString) {
        long returnValue = 0;
        try {
            SolrQuery query = new SolrQuery();

            query.setQuery(queryString);

            //IMPORTANT!Only use facets if needed.
            query.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.

            returnValue = connector.query(query, SolrRequest.METHOD.POST).getResults().getNumFound();

        } catch (SolrServerException e) {
            log.error("Error performing baseQuery on Solr server. Query: {}, Server address: {}", queryString, connector.getBaseURL(), e);
        }
        return returnValue;
    }

    protected HttpSolrServer createConnector(String serverUrl){
        HttpSolrServer solrServer = new HttpSolrServer(serverUrl);
        solrServer.setRequestWriter(new BinaryRequestWriter()); //To avoid http error code 413/414, due to monster URI. (and it is faster)
        return solrServer;
    }

    protected List<VideoInfo> processQueryResponse(QueryResponse response) {
        SolrDocumentList responseResults = response.getResults();
        responseResults.getNumFound();

        List<VideoInfo> result = new LinkedList<>();
        for (SolrDocument solrResult : responseResults) {
            VideoInfo video;

            String uuid = solrResult.getFirstValue("recordID").toString();
            String startTime = solrResult.getFirstValue("sort_year_asc").toString();
            String title = solrResult.getFirstValue("sort_title").toString();

            video = new VideoInfo(uuid,title,startTime);
            result.add(video);
        }
        return result;
    }

    /**
     * Performs a query in Solr index.
     * @param solrServer The server to query.
     * @param queryString The actual query that defines what to ask for and where.
     * @param startRow How fair into the index you want to start.
     * @param rows Total amount of rows to retrieve. Don't go over 1000!
     */
    protected QueryResponse querySolr(HttpSolrServer solrServer, String queryString, int startRow, int rows ) {
        if(rows > 1000) {
            return null;
        }
        try {
            SolrQuery query = new SolrQuery();

            query.setQuery(queryString);

            //For paging
            query.setRows(rows); //Fetch size. Do not go over 1000 unless you specify fields to fetch which does not include content_text
            query.setStart(startRow);

            //IMPORTANT!Only use facets if needed.
            query.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.

            query.setFields("recordID","sort_title", "sort_year_asc");

            query.addSort("sort_year_asc", SolrQuery.ORDER.asc);

            return solrServer.query(query, SolrRequest.METHOD.POST);
        } catch (SolrServerException e) {
            log.error("Error performing baseQuery on Solr server. Query: {}, Server address: {}", queryString, solrServer.getBaseURL(), e);
            return null;
        }
    }

    public String getBaseQuery() {
        return baseQuery;
    }

    public String getWithSRT() {
        return withSRT;
    }

    public String getWithoutSRT() {
        return withoutSRT;
    }

    @Override
    public void close() {
        this.connector.shutdown();
    }

}
