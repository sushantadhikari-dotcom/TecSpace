import api from "./api.js";

/** Downloads a file that needs login: fetches it with the token, then saves it. */
export async function downloadFile(path, filename) {
  const r = await api.get(path, { responseType: "blob" });
  const url = URL.createObjectURL(r.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/** For showing a protected image: returns a temporary browser URL for it. */
export async function blobUrl(path) {
  const r = await api.get(path, { responseType: "blob" });
  return URL.createObjectURL(r.data);
}
