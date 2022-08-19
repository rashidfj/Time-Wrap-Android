package com.peek.time.wrap.scan.timewrap.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.activities.ImageOpenActivity;
import com.peek.time.wrap.scan.timewrap.activities.SavedImagesActivity;
import com.peek.time.wrap.scan.timewrap.model.LiveDataModel;
import com.peek.time.wrap.scan.timewrap.model.SavedModel;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.viewHolder> {


    private final List<SavedModel> mList;
    private Context mContext;


    LiveDataModel mLiveModel;
    boolean isEnableTWS = false;
    boolean isSelectedAllTWS = false;
    List<SavedModel> selItemsTWS = new ArrayList<>();
    SparseBooleanArray sparseBooleanArrayTWS = new SparseBooleanArray();
    MenuItem mSelALL;


    public SavedAdapter(List<SavedModel> imagesList, Context context) {
        this.mList = imagesList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.cell_saved_sdapter, parent, false);

        mLiveModel = ViewModelProviders.of((FragmentActivity) mContext).get(LiveDataModel.class);

        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, @SuppressLint("RecyclerView") int position) {

        final SavedModel itemModel = mList.get(holder.getAdapterPosition());
        mContext = holder.itemView.getContext();

        Picasso.get().load(itemModel.getFile()).into(holder.ivThumb);

        holder.ivThumb.setOnClickListener(v -> {

            if (isEnableTWS) {
                SavedClickItem(holder, null);
            } else {
                Intent intent = new Intent(mContext, ImageOpenActivity.class);
                intent.putExtra("STR_IMAGE", mList.get(position).getPath());
                mContext.startActivity(intent);
            }
        });


        holder.ivShare.setOnClickListener(view -> {


            try {
//                holder.ivThumb.buildDrawingCache();
//                Bitmap bitmap = holder.ivThumb.getDrawingCache();
                BitmapDrawable drawable = (BitmapDrawable) holder.ivThumb.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                String path = MediaStore.Images.Media.insertImage(mContext.getContentResolver(), bitmap, "Title", null);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                mContext.startActivity(Intent.createChooser(share, "Share via"));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }


        });


        holder.btDownload.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                try {
                    if (itemModel.getFile().delete()) {
                        mList.remove(position);
                        notifyDataSetChanged();
                        Toast.makeText(mContext, "Deleted", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });


        if (sparseBooleanArrayTWS.get(holder.getAdapterPosition(), false)) {

            holder.selectedItem.setVisibility(View.VISIBLE);

        } else {
            holder.selectedItem.setVisibility(View.GONE);

        }

        holder.ivThumb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!isEnableTWS) {
                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

                            MenuInflater menuInflater = actionMode.getMenuInflater();
                            menuInflater.inflate(R.menu.menu_main, menu);
                            SavedImagesActivity.actionModeTWS = actionMode;
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                            mSelALL = menu.findItem(R.id.menu_select_all);

                            isEnableTWS = true;
                            SavedClickItem(holder, actionMode);

                            mLiveModel.getText().observe((LifecycleOwner) mContext
                                    , new Observer<String>() {
                                        @Override
                                        public void onChanged(String s) {
                                            actionMode.setTitle(String.format("%s Selected", s + "/" + mList.size()));

                                            if (selItemsTWS.isEmpty()) {
                                                mSelALL.setVisible(true);
                                            } else {
                                                mSelALL.setVisible(selItemsTWS.size() != mList.size());
                                            }

                                        }
                                    });

                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                            int id = menuItem.getItemId();
                            switch (id) {
                                case R.id.action_delete:

                                    androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(
                                            new ContextThemeWrapper(mContext, R.style.CustomAlertDialog));

                                    alert.setTitle("Delete");
                                    alert.setMessage("Are you sure you want to delete the selected item ?");
                                    alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            for (SavedModel s : selItemsTWS) {
                                                mList.remove(s);
                                                s.getFile().delete();
                                                notifyDataSetChanged();
                                            }
                                            if (actionMode != null) {
                                                actionMode.finish();
                                            }

                                        }
                                    });
                                    alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // close dialog
                                            dialog.cancel();
                                            if (actionMode != null) {
                                                actionMode.finish();
                                            }
                                        }
                                    });
                                    alert.show();
                                    break;


                                case R.id.menu_select_all:
                                    if (selItemsTWS.size() == mList.size()) {
                                        isSelectedAllTWS = false;
                                        selItemsTWS.clear();
                                        sparseBooleanArrayTWS.clear();
                                    } else {
                                        isSelectedAllTWS = true;
                                        selItemsTWS.clear();
                                        selItemsTWS.addAll(mList);
                                        for (int i = 0; i < mList.size(); i++) {
                                            sparseBooleanArrayTWS.put(i, true);
                                        }
                                        mSelALL.setVisible(false);
                                        /*      menu_select_all_2.setVisible(true);*/
                                    }
                                    mLiveModel.setText(String.valueOf(selItemsTWS.size()));
                                    notifyDataSetChanged();
                                    break;

                            }

                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode actionMode) {

                            isEnableTWS = false;
                            isSelectedAllTWS = false;
                            selItemsTWS.clear();
                            notifyDataSetChanged();
                            sparseBooleanArrayTWS.clear();
                        }
                    };

                    ((AppCompatActivity) view.getContext()).startActionMode(callback);
                }
                return true;

            }
        });


    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        RelativeLayout rlMain, selectedItem;
        ImageView ivThumb, btDownload, ivShare;

        public viewHolder(@NonNull View itemView) {
            super(itemView);


            rlMain = itemView.findViewById(R.id.rl_main);
            ivThumb = itemView.findViewById(R.id.thumb_whatsapp);
            btDownload = itemView.findViewById(R.id.download_whatsapp_btn);
            ivShare = itemView.findViewById(R.id.img_share_whatsapp);
            selectedItem = itemView.findViewById(R.id.selected_item);
        }
    }


    private void SavedClickItem(viewHolder holder, ActionMode mode) {

        SavedModel s = mList.get(holder.getAdapterPosition());
        if (holder.selectedItem.getVisibility() == View.GONE) {

            holder.selectedItem.setVisibility(View.VISIBLE);
            selItemsTWS.add(s);
            sparseBooleanArrayTWS.put(holder.getAdapterPosition(), true);
        } else {
            holder.selectedItem.setVisibility(View.GONE);
            selItemsTWS.remove(s);
            sparseBooleanArrayTWS.delete(holder.getAdapterPosition());

            if (mode != null) {
                mode.finish();

            }
        }
        mLiveModel.setText(String.valueOf(selItemsTWS.size()));
    }
}
