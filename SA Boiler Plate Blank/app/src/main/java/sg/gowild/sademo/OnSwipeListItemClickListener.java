package sg.gowild.sademo;

import android.view.View;

public interface OnSwipeListItemClickListener {
    public void OnClick(View view, int index);
    public boolean OnLongClick(View view, int index);
    public void OnControlClick(int rid,View view,int index);
}
