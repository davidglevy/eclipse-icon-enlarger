package testEclipse;

public enum ImageType {

	PNG(".png"),
	GIF(".gif");
	
	private String suffix;

	private ImageType(String fileName) {
		this.suffix = fileName;
	}

	public String getFileName() {
		return suffix;
	}
	
	public static ImageType findType(String fileName) {
		for (ImageType type : values()) {
			if (fileName.toLowerCase().endsWith(type.suffix)) {
				return type;
			}
		}
		
		return null;
	}
}
