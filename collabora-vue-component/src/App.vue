<template>
  <collabora-iframe
    v-if="wopiFileUrl && collaboraUrl"
    :access-token="accessToken"
    :access-token-t-t-l="accessTokenTTL"
    :collabora-url="collaboraUrl"
    :wopi-file-url="wopiFileUrl"
  ></collabora-iframe>
</template>
<script>
import CollaboraIframe from "./components/CollaboraIframe.vue";
import axios from "axios";

export default {
  name: "CollaboraOnlineVue",
  components: {
    CollaboraIframe,
  },
  data() {
    return {
      accessToken: null,
      accessTokenTTL: null,
      collaboraUrl: null,
      wopiFileUrl: null,
    };
  },
  created() {
    this.loadExample();
  },
  methods: {
    loadExample() {
      let nodeId = "0dde2d84-0ba6-4f6e-9f0a-0eb9f15d9888";
      this.getLoolUrl()
        .then((loolUrl) => {
          this.wopiFileUrl = encodeURI(
            `${loolUrl["lool_host_url"]}wopi/files/${nodeId}`
          );
          return this.getAccessToken(nodeId, "edit");
        })
        .then((getAccessToken) => {
          this.accessToken = getAccessToken["access_token"];
          this.accessTokenTTL = getAccessToken["access_token_ttl"];
          this.collaboraUrl = getAccessToken["wopi_src_url"];
        });
    },
    getLoolUrl() {
      let path = `http://localhost:8008/alfresco/service/lool/host/url`;
      return axios
        .get(path)
        .then((resp) => {
          console.log("lool url :");
          console.log(resp.data);
          return resp.data;
        })
        .catch((error) => {
          console.log(`erreur : ${error}`);
          return error;
        });
    },
    getAccessToken(nodeId, action) {
      let path = `http://localhost:8008/alfresco/service/lool/token?nodeRef=workspace://SpacesStore/${nodeId}&action=${action}`;
      return axios
        .get(path)
        .then((resp) => {
          console.log("access token :");
          console.log(resp.data);
          return resp.data;
        })
        .catch((error) => {
          console.log(`erreur : ${error}`);
          return error;
        });
    },
  },
};
</script>

<style></style>
