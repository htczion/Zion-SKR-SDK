package com.htc.wallet.skrsdk.drives.onedrive;

import com.google.gson.annotations.SerializedName;

public class AppFolderResponse {

    @SerializedName("@odata.context")
    private String _$OdataContext216;

    @SerializedName("createdDateTime")
    private String createdDateTime;

    @SerializedName("cTag")
    private String cTag;

    @SerializedName("eTag")
    private String eTag;

    @SerializedName("id")
    private String id;

    @SerializedName("lastModifiedDateTime")
    private String lastModifiedDateTime;

    @SerializedName("name")
    private String name;

    @SerializedName("size")
    private int size;

    @SerializedName("webUrl")
    private String webUrl;

    @SerializedName("createdBy")
    private CreatedByBean createdBy;

    @SerializedName("lastModifiedBy")
    private LastModifiedByBean lastModifiedBy;

    @SerializedName("parentReference")
    private ParentReferenceBean parentReference;

    @SerializedName("fileSystemInfo")
    private FileSystemInfoBean fileSystemInfo;

    @SerializedName("folder")
    private FolderBean folder;

    public String get_$OdataContext216() {
        return _$OdataContext216;
    }

    public void set_$OdataContext216(String _$OdataContext216) {
        this._$OdataContext216 = _$OdataContext216;
    }

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getCTag() {
        return cTag;
    }

    public void setCTag(String cTag) {
        this.cTag = cTag;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(String lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public CreatedByBean getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(CreatedByBean createdBy) {
        this.createdBy = createdBy;
    }

    public LastModifiedByBean getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(LastModifiedByBean lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public ParentReferenceBean getParentReference() {
        return parentReference;
    }

    public void setParentReference(ParentReferenceBean parentReference) {
        this.parentReference = parentReference;
    }

    public FileSystemInfoBean getFileSystemInfo() {
        return fileSystemInfo;
    }

    public void setFileSystemInfo(FileSystemInfoBean fileSystemInfo) {
        this.fileSystemInfo = fileSystemInfo;
    }

    public FolderBean getFolder() {
        return folder;
    }

    public void setFolder(FolderBean folder) {
        this.folder = folder;
    }

    public static class CreatedByBean {
        @SerializedName("application")
        private ApplicationBean application;

        @SerializedName("user")
        private UserBean user;

        public ApplicationBean getApplication() {
            return application;
        }

        public void setApplication(ApplicationBean application) {
            this.application = application;
        }

        public UserBean getUser() {
            return user;
        }

        public void setUser(UserBean user) {
            this.user = user;
        }

        public static class ApplicationBean {
            @SerializedName("displayName")
            private String displayName;

            @SerializedName("id")
            private String id;

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }

        public static class UserBean {
            @SerializedName("displayName")
            private String displayName;

            @SerializedName("id")
            private String id;

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
    }

    public static class LastModifiedByBean {
        @SerializedName("application")
        private ApplicationBeanX application;

        @SerializedName("user")
        private UserBeanX user;

        public ApplicationBeanX getApplication() {
            return application;
        }

        public void setApplication(ApplicationBeanX application) {
            this.application = application;
        }

        public UserBeanX getUser() {
            return user;
        }

        public void setUser(UserBeanX user) {
            this.user = user;
        }

        public static class ApplicationBeanX {
            @SerializedName("displayName")
            private String displayName;

            @SerializedName("id")
            private String id;

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }

        public static class UserBeanX {
            @SerializedName("displayName")
            private String displayName;

            @SerializedName("id")
            private String id;

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
    }

    public static class ParentReferenceBean {
        @SerializedName("driveId")
        private String driveId;

        @SerializedName("driveType")
        private String driveType;

        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("path")
        private String path;

        public String getDriveId() {
            return driveId;
        }

        public void setDriveId(String driveId) {
            this.driveId = driveId;
        }

        public String getDriveType() {
            return driveType;
        }

        public void setDriveType(String driveType) {
            this.driveType = driveType;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class FileSystemInfoBean {
        @SerializedName("createdDateTime")
        private String createdDateTime;

        @SerializedName("lastModifiedDateTime")
        private String lastModifiedDateTime;

        public String getCreatedDateTime() {
            return createdDateTime;
        }

        public void setCreatedDateTime(String createdDateTime) {
            this.createdDateTime = createdDateTime;
        }

        public String getLastModifiedDateTime() {
            return lastModifiedDateTime;
        }

        public void setLastModifiedDateTime(String lastModifiedDateTime) {
            this.lastModifiedDateTime = lastModifiedDateTime;
        }
    }

    public static class FolderBean {
        @SerializedName("childCount")
        private int childCount;

        @SerializedName("view")
        private ViewBean view;

        public int getChildCount() {
            return childCount;
        }

        public void setChildCount(int childCount) {
            this.childCount = childCount;
        }

        public ViewBean getView() {
            return view;
        }

        public void setView(ViewBean view) {
            this.view = view;
        }

        public static class ViewBean {
            @SerializedName("viewType")
            private String viewType;

            @SerializedName("sortBy")
            private String sortBy;

            @SerializedName("sortOrder")
            private String sortOrder;

            public String getViewType() {
                return viewType;
            }

            public void setViewType(String viewType) {
                this.viewType = viewType;
            }

            public String getSortBy() {
                return sortBy;
            }

            public void setSortBy(String sortBy) {
                this.sortBy = sortBy;
            }

            public String getSortOrder() {
                return sortOrder;
            }

            public void setSortOrder(String sortOrder) {
                this.sortOrder = sortOrder;
            }
        }
    }
}
