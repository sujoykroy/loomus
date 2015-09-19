package bk2suz.loomus;

/**
 * Created by sujoy on 15/9/15.
 */
public abstract class OnLoadListener<T>  {
    private boolean mIsCancelled = false;

    public void doCancel() {
        mIsCancelled = true;
    }

    public boolean checkIsCancelled() {
        return mIsCancelled;
    }

    public abstract void onLoad(T item);
}
