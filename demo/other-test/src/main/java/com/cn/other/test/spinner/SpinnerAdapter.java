package com.cn.other.test.spinner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.cn.other.test.R;

import java.util.List;

/**
 * Created by wt on 2021/1/4.
 */
public class SpinnerAdapter extends RecyclerView.Adapter<SpinnerAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private List<String> mObjects;
    private Context context;
    private int mSelectItem = 0;

    public SpinnerAdapter(Context context, List<String> list) {
        mInflater = LayoutInflater.from(context);
        this.mObjects = list;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_spinner, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (mObjects != null && mObjects.size() > 0) {
            holder.mTextView.setText(mObjects.get(position));
        }
        boolean isSelected = false;
        if (mSelectItem == position) {
            isSelected = true;
            holder.mImageView.setVisibility(View.VISIBLE);
//            holder.mTextView.setTextColor(context.getResources().getColor(R.color.x1e94da));
        }else {
            isSelected = false;
            holder.mImageView.setVisibility(View.INVISIBLE);
//            holder.mTextView.setTextColor(context.getResources().getColor(R.color.spinner_text_color));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.setOnItemClick(position);
            }
        });

        boolean finalIsSelected = isSelected;
        holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mOnItemClickListener.onFocusChange(holder.mImageView,holder.mTextView, hasFocus, finalIsSelected);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mObjects == null?0:mObjects.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;
        private ImageView mImageView;

        public ViewHolder(View view) {
            super(view);
            mTextView = view.findViewById(R.id.tvName);
            mImageView = view.findViewById(R.id.ivSelect);
        }
    }

    /**
     * 刷新选中状态
     * @param selIndex 选中项索引，-1 表示无选中项（用于自定义值场景）
     */
    public void refreshData(int selIndex){
        if (mObjects != null){
            // 允许 -1 表示无选中项
            if (selIndex < -1){
                selIndex = 0;
            }
            if (selIndex >= mObjects.size()){
                selIndex = mObjects.size() - 1;
            }
        }else{
            selIndex = 0;
        }

        mSelectItem = selIndex;
    }

    private OnItemClickListener mOnItemClickListener;
    public interface OnItemClickListener{
        void setOnItemClick(int position);
        void onFocusChange(ImageView v,TextView textView, boolean hasFocus,boolean isSelected);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        mOnItemClickListener = onItemClickListener;
    }

}
