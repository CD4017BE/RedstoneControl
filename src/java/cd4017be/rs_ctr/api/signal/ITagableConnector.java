package cd4017be.rs_ctr.api.signal;


/**
 * @author CD4017BE
 *
 */
public interface ITagableConnector extends IConnector {

	public void setTag(MountedSignalPort port, String tag);

	public String getTag();

	@Override
	default String displayInfo(MountedSignalPort port, int linkID) {
		String tag = getTag();
		return tag != null ? "\n\u00a7e" + tag : IConnector.super.displayInfo(port, linkID);
	}

}
