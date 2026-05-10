package pro.sketchware.third_party.kellinwood.logging.android;


import pro.sketchware.third_party.kellinwood.logging.Logger;
import pro.sketchware.third_party.kellinwood.logging.LoggerFactory;

public class AndroidLoggerFactory implements LoggerFactory
{
    protected static AndroidLoggerFactory instance = new AndroidLoggerFactory();

    private AndroidLoggerFactory() {}

    public static AndroidLoggerFactory getInstance() {
        return instance;
    }

	@Override
	public Logger getLogger(String category) {
		return new AndroidLogger( category);
	}

}
