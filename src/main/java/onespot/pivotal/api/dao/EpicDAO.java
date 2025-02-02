package onespot.pivotal.api.dao;

import com.google.common.collect.Multimap;
import onespot.pivotal.api.resources.Epic;
import onespot.pivotal.rest.JsonRestClient;

/**
 * Created by ian on 3/29/15.
 */
public class EpicDAO extends DAO {
    public EpicDAO(JsonRestClient jsonRestClient, String path, Multimap<String, String> params) {
        super(jsonRestClient, path, params);
    }

    public Epic get() {
        return jsonRestClient.get(Epic.class, path, params);
    }

    public void put(Epic epic) {
        jsonRestClient.put(Epic.class, path, params, epic);
    }
}
