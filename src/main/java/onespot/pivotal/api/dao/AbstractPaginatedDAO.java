package onespot.pivotal.api.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import onespot.pivotal.rest.JsonRestClient;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by ian on 3/30/15.
 */
public abstract class AbstractPaginatedDAO<R> extends DAO {
    public AbstractPaginatedDAO(JsonRestClient jsonRestClient, String path, Multimap<String, String> params) {
        super(jsonRestClient, path, params);
    }

    protected abstract Type getListTypeToken();

    public List<R> get() {
        return jsonRestClient.get(getListTypeToken(), path, params);
    }

    /**
     * Retrieve all items under this DAO using the pagination mechanism.
     * As this may require multiple calls to the pivotal API, it might take a while.
     *
     * @return
     */
    public List<R> getAll() {
        List<R> items = Lists.newArrayList();
        int position = 0;
        while (true) {
            // TODO(ic): Would be better to look at Tracker-Pagination-Total header
            this.offset(position).limit(100);
            List<R> retrievedItems = this.get();
            items.addAll(retrievedItems);
            int numberOfItemsReceived = retrievedItems.size();
            if (numberOfItemsReceived < 100) {
                break;
            }
            position += numberOfItemsReceived;
        }
        return items;
    }

    public AbstractPaginatedDAO<R> limit(int limit) {
        params.put("limit", Integer.toString(limit));
        return this;
    }

    public AbstractPaginatedDAO<R> offset(int offset) {
        params.put("offset", Integer.toString(offset));
        return this;
    }
}
