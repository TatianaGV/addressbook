import { ADD_ALERT, Caches, SUCCESS } from "../Common/Utils";
import * as tableActions from "../Table/TableActions";

export const GET_LIST = "GET_LIST";

export function asyncCommonCatch(action, error, dispatch) {
  const newAlert = {
    id: new Date().getTime(),
    type: "danger",
    headline: "Error occurred!",
    message: error.message,
  };
  dispatch({
    type: ADD_ALERT,
    alert: newAlert,
  });
}

export function clearPersonSelection(rows) {
  return (dispatch) => {
    dispatch({
      type: tableActions.ON_SELECT_ROW + Caches.PERSON_CACHE,
      row: rows,
      isSelected: false,
    });
  };
}

export function getList(url, filterDto = null, cacheName) {
  let isOk = false;
  return function (dispatch) {
    let headers = new Headers();
    headers.append("Accept", "application/json");
    headers.append("Content-Type", "application/json; charset=utf-8");

    fetch(url, {
      method: "post",
      headers: headers,
      credentials: "include",
      body: JSON.stringify(filterDto),
    })
      .then((response) => {
        ifNoAuthorizedRedirect(response);
        isOk = response.ok;
        return response.text();
      })
      .then((text) => {
        if (isOk) {
          let json = JSON.parse(text);
          dispatch({
            type: GET_LIST + cacheName + SUCCESS,
            data: json.data,
            fieldDescriptionMap: json.fieldDescriptionMap,
          });
        } else {
          dispatch({
            type: ADD_ALERT,
            alert: JSON.parse(text),
          });
        }
      })
      .catch((error) => {
        asyncCommonCatch(GET_LIST, error, dispatch);
      });
  };
}

export function ifNoAuthorizedRedirect(response) {
  if (response.status === 401 || response.status === 403) {
    window.location.hash = "#/login";
  }
}
